// SPDX-License-Identifier: Apache-2.0
import { Box, Grid } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import ConfirmationNumberOutlinedIcon from "@mui/icons-material/ConfirmationNumberOutlined";
import SpeedOutlinedIcon from "@mui/icons-material/SpeedOutlined";
import ErrorOutlineOutlinedIcon from "@mui/icons-material/ErrorOutlineOutlined";
import { PageHeader, DataTable, KpiCard, Card } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { opsApiStatsApi, type ApiStatRowDto } from "@/shared/api/ops";

export function ApiStats() {
  const { t } = useI18n();

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["api", "ops", "api-stats", { days: 7 }],
    queryFn: () => opsApiStatsApi.stats(7),
  });

  const summary = data?.summary;
  const items = data?.items ?? [];

  const cols = [
    {
      key: "path",
      title: t("common.path"),
      render: (r: ApiStatRowDto) => (
        <Box component="code" sx={{ fontSize: 12, fontFamily: "monospace" }}>
          {r.path}
        </Box>
      ),
    },
    { key: "calls", title: t("ops.apiStats.calls") },
    {
      key: "avgTimeMs",
      title: t("ops.apiStats.avgTime"),
      render: (r: ApiStatRowDto) => `${r.avgTimeMs.toFixed(0)} ms`,
    },
    {
      key: "errorRate",
      title: t("ops.apiStats.errorRate"),
      render: (r: ApiStatRowDto) => `${(r.errorRate * 100).toFixed(2)}%`,
    },
  ];

  return (
    <PageStack>
      <PageHeader title={t("ops.apiStats.title")} subtitle={t("ops.apiStats.subtitle")} />

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 4 }}>
          <KpiCard
            title={t("ops.apiStats.totalCalls")}
            value={summary?.totalCalls ?? "—"}
            icon={<ConfirmationNumberOutlinedIcon />}
            color="primary"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <KpiCard
            title={t("ops.apiStats.avgTime")}
            value={summary ? `${summary.avgTimeMs.toFixed(0)} ms` : "—"}
            icon={<SpeedOutlinedIcon />}
            color="success"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <KpiCard
            title={t("ops.apiStats.errorRate")}
            value={summary ? `${(summary.errorRate * 100).toFixed(2)}%` : "—"}
            icon={<ErrorOutlineOutlinedIcon />}
            color="warning"
          />
        </Grid>
      </Grid>

      <Card>
        <DataTable
          columns={cols}
          data={items}
          loading={isLoading}
          rowKey={(r) => r.path}
          error={error}
          onRetry={() => void refetch()}
        />
      </Card>
    </PageStack>
  );
}

export default ApiStats;
