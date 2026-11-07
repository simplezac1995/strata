// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Typography } from "@mui/material";
import {
  Chip,
  DataTable,
  ExportCsvButton,
  PageHeader,
  Pagination,
  QueryForm,
  StatusChip,
  TextField,
} from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { auditLogQueryApi, type AuditLogQuery } from "@/shared/api/files";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { exportCsvFromColumns, type CsvColumn } from "@/shared/utils/csv";
import { PageStack } from "@/shared/pageKit/PageLayout";
import type { AuditLogDto } from "@/shared/types";

type Filters = Pick<AuditLogQuery, "operator" | "from" | "to">;

const EMPTY: Filters = { operator: "", from: "", to: "" };

export function AuditLogs() {
  const { t } = useI18n();
  const [draft, setDraft] = useState<Filters>(EMPTY);
  const [applied, setApplied] = useState<Filters>(EMPTY);

  const { rows, total, page, setPage, pageSize, setPageSize, loading, reload, onSearch, error } =
    usePagedQuery<AuditLogDto>(["api", "system", "audit-logs", applied], (pageIndex, size) =>
      auditLogQueryApi.page({ page: pageIndex, size, ...applied }),
    );

  const csvColumns: CsvColumn<AuditLogDto>[] = [
    { title: t("system.auditLogs.eventType"), cell: (r) => r.eventType },
    { title: t("common.module"), cell: (r) => r.moduleName },
    { title: t("common.action"), cell: (r) => r.actionName },
    { title: t("common.operator"), cell: (r) => r.operatorName ?? "" },
    { title: t("system.common.ip"), cell: (r) => r.ipAddress ?? "" },
    {
      title: t("common.status"),
      cell: (r) => (r.success ? t("common.enabled") : t("common.disabled")),
    },
    { title: t("common.detail"), cell: (r) => r.detail ?? "" },
    {
      title: t("common.createdAt"),
      cell: (r) => (r.createdAt ? new Date(r.createdAt).toLocaleString() : ""),
    },
  ];

  const cols = [
    {
      key: "eventType",
      title: t("system.auditLogs.eventType"),
      render: (r: AuditLogDto) => <Chip label={r.eventType} color="primary" />,
    },
    { key: "moduleName", title: t("common.module") },
    { key: "actionName", title: t("common.action") },
    { key: "operatorName", title: t("common.operator") },
    {
      key: "ipAddress",
      title: t("system.common.ip"),
      render: (r: AuditLogDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.secondary">
          {r.ipAddress ?? t("common.emDash")}
        </Typography>
      ),
    },
    {
      key: "success",
      title: t("common.status"),
      render: (r: AuditLogDto) => <StatusChip enabled={r.success} />,
    },
    {
      key: "detail",
      title: t("common.detail"),
      render: (r: AuditLogDto) => (
        <Typography variant="body2" color="text.secondary" noWrap sx={{ maxWidth: 280 }}>
          {r.detail ?? t("common.emDash")}
        </Typography>
      ),
    },
    {
      key: "createdAt",
      title: t("common.createdAt"),
      render: (r: AuditLogDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.secondary">
          {new Date(r.createdAt).toLocaleString()}
        </Typography>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("nav.audit.logs")}
        subtitle={t("system.auditLogs.detailedAuditTrailForCompliance")}
        actions={
          <ExportCsvButton
            onClick={() => exportCsvFromColumns("audit-logs.csv", csvColumns, rows)}
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
          label={t("common.operator")}
          placeholder={t("system.logs.operatorPlaceholder")}
          value={draft.operator ?? ""}
          onChange={(e) => setDraft((d) => ({ ...d, operator: e.target.value }))}
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
        tableId="logs.audit"
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
