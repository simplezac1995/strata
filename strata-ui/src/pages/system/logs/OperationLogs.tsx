// SPDX-License-Identifier: Apache-2.0
import { useMemo, useState } from "react";
import { Typography } from "@mui/material";
import {
  DataTable,
  ExportCsvButton,
  PageHeader,
  Pagination,
  QueryForm,
  Select,
  TextField,
} from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { operationLogQueryApi, type OperationLogQuery } from "@/shared/api/files";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { exportCsvFromColumns, type CsvColumn } from "@/shared/utils/csv";
import { PageStack } from "@/shared/pageKit/PageLayout";
import type { OperationLogDto } from "@/shared/types";

type Filters = Pick<OperationLogQuery, "username" | "module" | "action" | "from" | "to">;

const EMPTY: Filters = { username: "", module: "", action: "", from: "", to: "" };

export function OperationLogs() {
  const { t } = useI18n();
  const [draft, setDraft] = useState<Filters>(EMPTY);
  const [applied, setApplied] = useState<Filters>(EMPTY);

  const { rows, total, page, setPage, pageSize, setPageSize, loading, reload, onSearch, error } =
    usePagedQuery<OperationLogDto>(
      ["api", "system", "operation-logs", applied],
      (pageIndex, size) => operationLogQueryApi.page({ page: pageIndex, size, ...applied }),
    );

  // Distinct option values harvested from the currently loaded page.
  const moduleOptions = useMemo(
    () => [...new Set(rows.map((r) => r.moduleName).filter(Boolean))],
    [rows],
  );
  const actionOptions = useMemo(
    () => [...new Set(rows.map((r) => r.actionName).filter(Boolean))],
    [rows],
  );

  const csvColumns: CsvColumn<OperationLogDto>[] = [
    { title: t("common.module"), cell: (r) => r.moduleName },
    { title: t("common.action"), cell: (r) => r.actionName },
    { title: t("common.operator"), cell: (r) => r.operatorName ?? "" },
    { title: t("common.detail"), cell: (r) => r.detail ?? "" },
    {
      title: t("common.createdAt"),
      cell: (r) => (r.createdAt ? new Date(r.createdAt).toLocaleString() : ""),
    },
  ];

  const cols = [
    { key: "moduleName", title: t("common.module") },
    { key: "actionName", title: t("common.action") },
    { key: "operatorName", title: t("common.operator") },
    {
      key: "detail",
      title: t("common.detail"),
      render: (r: OperationLogDto) => (
        <Typography variant="body2" color="text.secondary" noWrap sx={{ maxWidth: 320 }}>
          {r.detail ?? t("common.emDash")}
        </Typography>
      ),
    },
    {
      key: "createdAt",
      title: t("common.createdAt"),
      render: (r: OperationLogDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.secondary">
          {new Date(r.createdAt).toLocaleString()}
        </Typography>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("nav.operation.logs")}
        subtitle={t("system.operationLogs.trackAllSystemOperations")}
        actions={
          <ExportCsvButton
            onClick={() => exportCsvFromColumns("operation-logs.csv", csvColumns, rows)}
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
          value={draft.username ?? ""}
          onChange={(e) => setDraft((d) => ({ ...d, username: e.target.value }))}
        />
        <Select
          label={t("common.module")}
          value={draft.module ?? ""}
          onChange={(e) => setDraft((d) => ({ ...d, module: e.target.value }))}
          options={[
            { value: "", label: t("common.all") },
            ...moduleOptions.map((m) => ({ value: m, label: m })),
          ]}
        />
        <Select
          label={t("common.action")}
          value={draft.action ?? ""}
          onChange={(e) => setDraft((d) => ({ ...d, action: e.target.value }))}
          options={[
            { value: "", label: t("common.all") },
            ...actionOptions.map((a) => ({ value: a, label: a })),
          ]}
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
        tableId="logs.operation"
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
