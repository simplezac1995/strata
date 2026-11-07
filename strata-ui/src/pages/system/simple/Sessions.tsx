// SPDX-License-Identifier: Apache-2.0
import { Box, Typography } from "@mui/material";
import { ExportCsvButton, PageHeader, DataTable, Alert } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { loginLogApi } from "@/shared/api/system";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { exportCsvFromColumns, type CsvColumn } from "@/shared/utils/csv";
import type { LoginLogDto } from "@/shared/types";
import { PageStack } from "@/shared/pageKit/PageLayout";

export function Sessions() {
  const { t } = useI18n();
  const { data: sessions, loading } = useListQuery<LoginLogDto>(
    ["api", "system", "sessions"],
    async () => {
      const res = await loginLogApi.page({ status: "success", page: 0, size: 50 });
      return res.content;
    },
  );

  const csvColumns: CsvColumn<LoginLogDto>[] = [
    { title: t("common.username"), cell: (s) => s.username },
    { title: t("system.sessions.client"), cell: (s) => s.browser ?? "" },
    { title: t("system.common.ip"), cell: (s) => s.ip ?? "" },
    {
      title: t("system.sessions.lastActiveAt"),
      cell: (s) => (s.createdAt ? new Date(s.createdAt).toLocaleString() : ""),
    },
  ];

  const cols = [
    { key: "username", title: t("common.username") },
    {
      key: "client",
      title: t("system.sessions.client"),
      render: (r: LoginLogDto) => (
        <Box
          component="code"
          sx={{
            fontSize: 12,
            fontFamily: "monospace",
            bgcolor: "action.hover",
            px: 0.75,
            borderRadius: 1,
          }}
        >
          {r.browser ?? t("common.emDash")}
        </Box>
      ),
    },
    {
      key: "ip",
      title: t("system.common.ip"),
      render: (r: LoginLogDto) => (
        <Box
          component="code"
          sx={{ fontSize: 12, fontFamily: "monospace", color: "text.secondary" }}
        >
          {r.ip ?? t("common.emDash")}
        </Box>
      ),
    },
    {
      key: "userAgent",
      title: t("system.sessions.userAgent"),
      render: (r: LoginLogDto) => (
        <Typography variant="caption" color="text.secondary">
          {r.browser ?? t("common.emDash")}
        </Typography>
      ),
    },
    {
      key: "lastActiveAt",
      title: t("system.sessions.lastActiveAt"),
      render: (r: LoginLogDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.secondary">
          {new Date(r.createdAt).toLocaleString()}
        </Typography>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("system.sessions.activeSessions")}
        subtitle={t("system.sessions.currentlyActiveUserSessions")}
        actions={
          <ExportCsvButton
            onClick={() => exportCsvFromColumns("sessions.csv", csvColumns, sessions)}
          />
        }
      />
      <Box sx={{ mb: 2 }}>
        <Alert type="info">{t("system.sessions.placeholder")}</Alert>
      </Box>
      <DataTable
        columns={cols}
        data={sessions}
        loading={loading}
        rowKey={(r) => r.id}
        tableId="sessions"
      />
    </PageStack>
  );
}

export default Sessions;
