// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, Typography } from "@mui/material";
import {
  Chip,
  DataTable,
  ExportCsvButton,
  PageHeader,
  Pagination,
  QueryForm,
  Select,
  TextField,
} from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { loginLogQueryApi, type LoginLogQuery } from "@/shared/api/files";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { exportCsvFromColumns, type CsvColumn } from "@/shared/utils/csv";
import { PageStack } from "@/shared/pageKit/PageLayout";
import type { LoginLogDto } from "@/shared/types";

type Filters = Pick<LoginLogQuery, "username" | "status" | "ip" | "from" | "to">;

const EMPTY: Filters = { username: "", status: "", ip: "", from: "", to: "" };

export function LoginLogs() {
  const { t } = useI18n();
  const [draft, setDraft] = useState<Filters>(EMPTY);
  const [applied, setApplied] = useState<Filters>(EMPTY);

  const { rows, total, page, setPage, pageSize, setPageSize, loading, reload, onSearch, error } =
    usePagedQuery<LoginLogDto>(["api", "system", "login-logs", applied], (pageIndex, size) =>
      loginLogQueryApi.page({ page: pageIndex, size, ...applied }),
    );

  const csvColumns: CsvColumn<LoginLogDto>[] = [
    { title: t("common.username"), cell: (r) => r.username },
    { title: t("system.loginLogs.ipAddress"), cell: (r) => r.ip ?? "" },
    { title: t("system.loginLogs.location"), cell: (r) => r.location ?? "" },
    { title: t("system.loginLogs.browser"), cell: (r) => r.browser ?? "" },
    { title: t("common.status"), cell: (r) => r.status },
    {
      title: t("system.loginLogs.date"),
      cell: (r) => (r.createdAt ? new Date(r.createdAt).toLocaleString() : ""),
    },
  ];

  const cols = [
    { key: "username", title: t("common.username") },
    {
      key: "ip",
      title: t("system.loginLogs.ipAddress"),
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
      key: "location",
      title: t("system.loginLogs.location"),
      render: (r: LoginLogDto) => r.location ?? t("common.emDash"),
    },
    {
      key: "browser",
      title: t("system.loginLogs.browser"),
      render: (r: LoginLogDto) => r.browser ?? t("common.emDash"),
    },
    {
      key: "status",
      title: t("common.status"),
      render: (r: LoginLogDto) => (
        <Chip
          label={
            r.status === "success" ? t("system.loginLogs.success") : t("system.loginLogs.failed")
          }
          color={r.status === "success" ? "success" : "error"}
        />
      ),
    },
    {
      key: "createdAt",
      title: t("system.loginLogs.date"),
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
        title={t("nav.login.logs")}
        subtitle={t("system.loginLogs.trackAllLoginAttemptsAndSessions")}
        actions={
          <ExportCsvButton
            onClick={() => exportCsvFromColumns("login-logs.csv", csvColumns, rows)}
          />
        }
      />
      <QueryForm
        onSearch={() => {
          setApplied({ ...draft });
          onSearch();
        }}
        onReset={() => {
          setDraft(EMPTY);
          setApplied(EMPTY);
          setPage(1);
        }}
      >
        <TextField
          label={t("common.username")}
          placeholder={t("common.searchPlaceholder")}
          value={draft.username ?? ""}
          onChange={(e) => setDraft((d) => ({ ...d, username: e.target.value }))}
        />
        <Select
          label={t("common.status")}
          value={draft.status ?? ""}
          onChange={(e) => setDraft((d) => ({ ...d, status: e.target.value }))}
          options={[
            { value: "", label: t("common.all") },
            { value: "success", label: t("system.loginLogs.success") },
            { value: "fail", label: t("system.loginLogs.failed") },
          ]}
        />
        <TextField
          label={t("system.loginLogs.ipAddress")}
          placeholder={t("system.logs.ipPlaceholder")}
          value={draft.ip ?? ""}
          onChange={(e) => setDraft((d) => ({ ...d, ip: e.target.value }))}
        />
        <TextField
          label={t("system.logs.from")}
          type="date"
          value={draft.from ?? ""}
          onChange={(e) => setDraft((d) => ({ ...d, from: e.target.value }))}
          slotProps={{ inputLabel: { shrink: true } }}
        />
        <TextField
          label={t("system.logs.to")}
          type="date"
          value={draft.to ?? ""}
          onChange={(e) => setDraft((d) => ({ ...d, to: e.target.value }))}
          slotProps={{ inputLabel: { shrink: true } }}
        />
      </QueryForm>
      <DataTable
        columns={cols}
        data={rows}
        loading={loading}
        rowKey={(r) => r.id}
        tableId="logs.login"
        error={error}
        onRetry={reload}
      />
      <Pagination
        page={page}
        pageSize={pageSize}
        total={total}
        onPage={setPage}
        onPageSize={setPageSize}
      />
    </PageStack>
  );
}
