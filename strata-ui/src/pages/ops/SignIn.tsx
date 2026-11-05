// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import { Box, Stack, Typography } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { Button, TextField, Card, PageHeader, DataTable, Pagination } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { opsSignInApi, type SignInDayConfigDto, type SignInRecordDto } from "@/shared/api/ops";

export function SignIn() {
  const { t } = useI18n();

  const {
    data: config,
    isLoading: configLoading,
    refetch: reloadConfig,
  } = useQuery({
    queryKey: ["api", "ops", "signin", "config"],
    queryFn: () => opsSignInApi.getConfig(),
  });

  const [rows, setRows] = useState<SignInDayConfigDto[]>([]);
  useEffect(() => {
    if (config) setRows(config.map((c) => ({ ...c })));
  }, [config]);

  const saveConfigMutation = useMutationWithToast(
    (payload: SignInDayConfigDto[]) => opsSignInApi.saveConfig(payload),
    { success: "toast.saved", failure: "toast.saveFailed" },
    { onSuccess: () => reloadConfig() },
  );

  const checkinMutation = useMutationWithToast(
    () => opsSignInApi.checkin(),
    { success: "ops.signin.checkedIn", failure: "ops.signin.checkinFailed" },
    { onSuccess: () => recordsReload() },
  );

  const {
    rows: records,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading: recordsLoading,
    error,
    reload: recordsReload,
  } = usePagedQuery<SignInRecordDto>(["api", "ops", "signin", "records"], opsSignInApi.records);

  function updateRow(day: number, patch: Partial<SignInDayConfigDto>) {
    setRows((prev) => prev.map((r) => (r.day === day ? { ...r, ...patch } : r)));
  }

  const recordCols = [
    { key: "username", title: t("common.username") },
    {
      key: "signInDate",
      title: t("ops.signin.date"),
      render: (r: SignInRecordDto) => (
        <Typography variant="caption" color="text.secondary">
          {r.signInDate}
        </Typography>
      ),
    },
    { key: "day", title: t("ops.signin.day") },
    { key: "rewardPoints", title: t("ops.signin.rewardPoints") },
    { key: "rewardMinor", title: t("ops.signin.rewardMinor") },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("ops.signin.title")}
        subtitle={t("ops.signin.subtitle")}
        actions={
          <Button
            variant="contained"
            disabled={checkinMutation.isPending}
            onClick={() => void checkinMutation.mutateAsync()}
          >
            {checkinMutation.isPending ? t("common.loading") : t("ops.signin.checkin")}
          </Button>
        }
      />

      <Card>
        <Stack
          direction="row"
          sx={{ alignItems: "center", justifyContent: "space-between", mb: 2 }}
        >
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            {t("ops.signin.configTitle")}
          </Typography>
          <Button
            variant="contained"
            disabled={saveConfigMutation.isPending || configLoading}
            onClick={() => void saveConfigMutation.mutateAsync(rows)}
          >
            {saveConfigMutation.isPending ? t("common.saving") : t("common.saveChanges")}
          </Button>
        </Stack>
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))",
            gap: 2,
          }}
        >
          {rows.map((row) => (
            <Card key={row.day} padding={2}>
              <Stack spacing={1.5}>
                <Typography variant="subtitle2">
                  {t("ops.signin.day")} {row.day}
                </Typography>
                <TextField
                  label={t("ops.signin.rewardPoints")}
                  type="number"
                  size="small"
                  value={row.rewardPoints}
                  onChange={(e) => updateRow(row.day, { rewardPoints: Number(e.target.value) })}
                />
                <TextField
                  label={t("ops.signin.rewardMinor")}
                  type="number"
                  size="small"
                  value={row.rewardMinor}
                  onChange={(e) => updateRow(row.day, { rewardMinor: Number(e.target.value) })}
                />
              </Stack>
            </Card>
          ))}
        </Box>
      </Card>

      <DataTable
        columns={recordCols}
        data={records}
        loading={recordsLoading}
        rowKey={(r) => r.id}
        tableId="ops-signin-records"
        error={error}
        onRetry={recordsReload}
      />
      <Pagination
        page={page}
        pageSize={pageSize}
        total={total}
        onPage={setPage}
        onPageSize={(size: number) => {
          setPageSize(size);
          setPage(1);
        }}
      />
    </PageStack>
  );
}

export default SignIn;
