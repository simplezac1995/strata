// SPDX-License-Identifier: Apache-2.0
import { Box, Grid, Stack, Typography } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import StorageOutlinedIcon from "@mui/icons-material/StorageOutlined";
import Grid3x3OutlinedIcon from "@mui/icons-material/Grid3x3Outlined";
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import MemoryOutlinedIcon from "@mui/icons-material/MemoryOutlined";
import MemoryOutlinedIcon2 from "@mui/icons-material/Memory";
import AccessTimeOutlinedIcon from "@mui/icons-material/AccessTimeOutlined";
import { Button, Card, PageHeader } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { opsHealthApi, type ComponentHealthDto, type HealthState } from "@/shared/api/ops";

function stateColor(state: HealthState): "success" | "error" | "action" {
  if (state === "up") return "success";
  if (state === "down") return "error";
  return "action";
}

function formatUptime(ms: number): string {
  if (!Number.isFinite(ms) || ms < 0) return "—";
  const seconds = Math.floor(ms / 1000);
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (days > 0) return `${days}d ${hours}h ${minutes}m`;
  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m`;
}

function HealthCard({
  title,
  icon,
  health,
  detail,
}: {
  title: string;
  icon: React.ReactNode;
  health: ComponentHealthDto | undefined;
  detail: string;
}) {
  const state = health?.status ?? "unknown";
  const dotColor = stateColor(state);
  return (
    <Card>
      <Stack spacing={1.5}>
        <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
          <Box
            sx={{
              width: 40,
              height: 40,
              borderRadius: 2,
              bgcolor: `${dotColor}.lighter`,
              color: `${dotColor}.main`,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            {icon}
          </Box>
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
              {title}
            </Typography>
          </Box>
          <Box
            sx={{
              width: 10,
              height: 10,
              borderRadius: "50%",
              bgcolor: `${dotColor}.main`,
            }}
          />
        </Stack>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>
          {detail}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          {health?.message ?? state}
        </Typography>
      </Stack>
    </Card>
  );
}

export function HealthCheck() {
  const { t } = useI18n();
  const { data, isLoading, error, refetch, isFetching } = useQuery({
    queryKey: ["api", "ops", "health"],
    queryFn: () => opsHealthApi.check(),
    refetchInterval: 30_000,
  });

  return (
    <PageStack>
      <PageHeader
        title={t("ops.health.title")}
        subtitle={t("ops.health.subtitle")}
        actions={
          <Button variant="outlined" onClick={() => void refetch()} disabled={isFetching}>
            {isFetching ? t("common.loading") : t("common.refresh")}
          </Button>
        }
      />
      {error ? (
        <Typography variant="body2" color="error">
          {t("common.loadFailed")}
        </Typography>
      ) : null}
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <HealthCard
            title={t("ops.health.db")}
            icon={<StorageOutlinedIcon />}
            health={data?.db}
            detail={
              data?.db?.responseTimeMs != null ? `${data.db.responseTimeMs.toFixed(0)} ms` : "—"
            }
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <HealthCard
            title={t("ops.health.redis")}
            icon={<Grid3x3OutlinedIcon />}
            health={data?.redis}
            detail={
              data?.redis?.responseTimeMs != null
                ? `${data.redis.responseTimeMs.toFixed(0)} ms`
                : "—"
            }
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <HealthCard
            title={t("ops.health.disk")}
            icon={<SaveOutlinedIcon />}
            health={data?.disk}
            detail={data?.disk?.usage != null ? `${Math.round(data.disk.usage)}%` : "—"}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <HealthCard
            title={t("ops.health.cpu")}
            icon={<MemoryOutlinedIcon />}
            health={data?.cpu}
            detail={data?.cpu?.usage != null ? `${Math.round(data.cpu.usage)}%` : "—"}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <HealthCard
            title={t("ops.health.memory")}
            icon={<MemoryOutlinedIcon2 />}
            health={data?.memory}
            detail={data?.memory?.usage != null ? `${Math.round(data.memory.usage)}%` : "—"}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <HealthCard
            title={t("ops.health.uptime")}
            icon={<AccessTimeOutlinedIcon />}
            health={undefined}
            detail={data ? formatUptime(data.uptimeMs) : isLoading ? "—" : "—"}
          />
        </Grid>
      </Grid>
    </PageStack>
  );
}

export default HealthCheck;
