// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, Grid, Stack, Typography } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { Alert, Button, DataTable, PageHeader, QueryForm, TextField } from "@/components/ui/index";
import { cacheApi } from "@/shared/api/system";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import type { CacheKeyDto, CacheStatusDto } from "@/shared/types";
import { ContentCard, PageStack } from "@/shared/pageKit/PageLayout";

function formatTtl(seconds: number | null, persistLabel: string): string {
  if (seconds === null || seconds < 0) return persistLabel;
  return `${seconds}s`;
}

function formatHitRate(rate: number | null | undefined): string {
  if (rate === null || rate === undefined) return "—";
  return `${Math.round(rate * 1000) / 10}%`;
}

export function CacheMonitor() {
  const { t } = useI18n();
  const canDelete = useAuthStore((s) => s.hasAuthority("system:cache:delete"));
  const [pattern, setPattern] = useState("");

  const {
    data: keys,
    loading,
    reload,
  } = useListQuery<CacheKeyDto>(["api", "system", "cache", "keys", { pattern }], () =>
    cacheApi.keys(pattern || undefined),
  );

  const { data: status } = useQuery<CacheStatusDto>({
    queryKey: ["api", "system", "cache", "status"],
    queryFn: () => cacheApi.status(),
  });

  const deleteKeyMutation = useMutationWithToast(
    (key: string) => cacheApi.deleteKey(key),
    { success: "toast.deleted", failure: "toast.deleteFailed" },
    {
      onSuccess: () => {
        reload();
      },
    },
  );

  const flushMutation = useMutationWithToast(
    () => cacheApi.flush(),
    { success: "toast.deleted", failure: "toast.deleteFailed" },
    {
      onSuccess: () => {
        reload();
      },
    },
  );

  const cols = [
    {
      key: "key",
      title: t("tools.cacheMonitor.cacheKey"),
      render: (row: CacheKeyDto) => (
        <Box component="code" sx={{ fontSize: 12, fontFamily: "monospace", color: "primary.main" }}>
          {row.key}
        </Box>
      ),
    },
    { key: "type", title: t("common.typeRequired") },
    {
      key: "ttl",
      title: "TTL",
      render: (row: CacheKeyDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.secondary">
          {formatTtl(row.ttlSeconds, t("tools.cacheMonitor.persist"))}
        </Typography>
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (row: CacheKeyDto) =>
        canDelete ? (
          <Button
            variant="text"
            size="sm"
            color="danger"
            onClick={() => void deleteKeyMutation.mutateAsync(row.key).catch(() => {})}
          >
            {t("tools.cacheMonitor.evict")}
          </Button>
        ) : null,
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("nav.cache.monitor")}
        subtitle={t("tools.cacheMonitor.redisCacheKeysAndStatistics")}
        actions={
          <Stack direction="row" spacing={1}>
            {canDelete && (
              <Button
                variant="outlined"
                color="danger"
                onClick={() => void flushMutation.mutateAsync().catch(() => {})}
              >
                {t("tools.cacheMonitor.flushAll")}
              </Button>
            )}
            <Button variant="outlined" color="default" onClick={() => void reload()}>
              {t("common.refresh")}
            </Button>
          </Stack>
        }
      />
      {status?.source === "local" && status.stub ? (
        <Alert type="warning">{t("tools.cacheMonitor.localStub")}</Alert>
      ) : null}
      {status?.source === "redis" && !status.stub ? (
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <ContentCard>
              <Typography variant="caption" color="text.secondary">
                {t("tools.cacheMonitor.redisVersion")}
              </Typography>
              <Typography variant="h6" sx={{ fontWeight: 700 }}>
                {status.redisVersion ?? "—"}
              </Typography>
            </ContentCard>
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <ContentCard>
              <Typography variant="caption" color="text.secondary">
                {t("tools.cacheMonitor.usedMemory")}
              </Typography>
              <Typography variant="h6" sx={{ fontWeight: 700 }}>
                {status.usedMemory ?? "—"}
              </Typography>
            </ContentCard>
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <ContentCard>
              <Typography variant="caption" color="text.secondary">
                {t("tools.cacheMonitor.connectedClients")}
              </Typography>
              <Typography variant="h6" sx={{ fontWeight: 700 }}>
                {status.connectedClients ?? "—"}
              </Typography>
            </ContentCard>
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <ContentCard>
              <Typography variant="caption" color="text.secondary">
                {t("tools.cacheMonitor.hitRate")}
              </Typography>
              <Typography variant="h6" sx={{ fontWeight: 700 }}>
                {formatHitRate(status.hitRate)}
              </Typography>
            </ContentCard>
          </Grid>
        </Grid>
      ) : null}
      <QueryForm onSearch={() => void reload()}>
        <TextField
          label={t("tools.cacheMonitor.pattern")}
          value={pattern}
          onChange={(e) => setPattern(e.target.value)}
          placeholder="*"
        />
      </QueryForm>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <ContentCard>
            <Typography variant="caption" color="text.secondary">
              {t("tools.cacheMonitor.totalKeys")}
            </Typography>
            <Typography variant="h5" sx={{ fontWeight: 700 }}>
              {status?.totalKeys ?? keys.length}
            </Typography>
          </ContentCard>
        </Grid>
      </Grid>
      <DataTable columns={cols} data={keys} loading={loading} rowKey={(row) => row.key} />
    </PageStack>
  );
}

export default CacheMonitor;
