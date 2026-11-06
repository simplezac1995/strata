// SPDX-License-Identifier: Apache-2.0
import { useMemo, useState } from "react";
import { Box, Stack, Typography } from "@mui/material";
import { HistoryOutlined, UndoOutlined } from "@mui/icons-material";
import {
  Alert,
  Button,
  DataTable,
  PageHeader,
  Select,
  TextField,
  Tooltip,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { auditApi, type AuditLogChangeDto } from "@/shared/api/audit";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { formatDateTime } from "@/shared/utils/date";

const KEY = ["api", "audit", "changes"];

/**
 * Audit log field-level changes. Rows are clickable to expand the full
 * old-vs-new diff below the table. The rollback action is rendered but kept
 * disabled until the backend rollback UX is product-approved.
 */
export function AuditChanges() {
  const { t } = useI18n();
  const [entity, setEntity] = useState("");
  const [entityId, setEntityId] = useState("");
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const changes = usePagedQuery<AuditLogChangeDto>([...KEY, { entity, entityId }], (page, size) =>
    auditApi.changesPage({ entity, entityId }, page, size),
  );

  // Entity filter options are derived from the rows on screen — no guessing.
  const entityOptions = useMemo(() => {
    const set = new Set<string>();
    for (const r of changes.rows) {
      if (r.entityName) set.add(r.entityName);
    }
    return [
      { value: "", label: t("common.all") },
      ...[...set].map((v) => ({ value: v, label: v })),
    ];
  }, [changes.rows, t]);

  const expanded = useMemo(
    () => changes.rows.find((r) => r.id === expandedId) ?? null,
    [changes.rows, expandedId],
  );

  const columns = [
    { key: "entityName", title: t("audit.entity"), render: (r: AuditLogChangeDto) => r.entityName },
    { key: "entityId", title: t("audit.entityId"), render: (r: AuditLogChangeDto) => r.entityId },
    { key: "fieldName", title: t("audit.field"), render: (r: AuditLogChangeDto) => r.fieldName },
    {
      key: "oldValue",
      title: t("audit.oldValue"),
      render: (r: AuditLogChangeDto) => (
        <Typography
          variant="body2"
          color="error"
          noWrap
          sx={{ maxWidth: 200, fontFamily: "monospace" }}
        >
          {r.oldValue || "—"}
        </Typography>
      ),
    },
    {
      key: "newValue",
      title: t("audit.newValue"),
      render: (r: AuditLogChangeDto) => (
        <Typography
          variant="body2"
          color="success.main"
          noWrap
          sx={{ maxWidth: 200, fontFamily: "monospace" }}
        >
          {r.newValue || "—"}
        </Typography>
      ),
    },
    {
      key: "createdAt",
      title: t("common.createdAt"),
      render: (r: AuditLogChangeDto) => (
        <Typography variant="caption" color="text.secondary" sx={{ fontFamily: "monospace" }}>
          {formatDateTime(r.createdAt)}
        </Typography>
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: () => (
        <Tooltip title={t("audit.rollbackPending")}>
          <span>
            <Button variant="outlined" size="sm" disabled>
              <UndoOutlined fontSize="small" sx={{ mr: 0.5 }} />
              {t("audit.rollback")}
            </Button>
          </span>
        </Tooltip>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader title={t("nav.auditChanges")} subtitle={t("audit.subtitle")} />

      <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
        <Select
          label={t("audit.entity")}
          value={entity}
          onChange={(e) => {
            setEntity(e.target.value);
            changes.setPage(1);
          }}
          options={entityOptions}
          sx={{ minWidth: 200 }}
        />
        <TextField
          label={t("audit.entityId")}
          value={entityId}
          onChange={(e) => setEntityId(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") changes.onSearch();
          }}
          sx={{ minWidth: 220 }}
        />
        <Button variant="outlined" color="default" onClick={changes.onSearch}>
          {t("common.search")}
        </Button>
      </Stack>

      <DataTable
        columns={columns}
        data={changes.rows}
        loading={changes.loading}
        rowKey={(r) => r.id}
        total={changes.total}
        page={changes.page}
        pageSize={changes.pageSize}
        onPageChange={changes.setPage}
        onPageSizeChange={changes.setPageSize}
        tableId="audit.changes"
        error={changes.error}
        onRetry={changes.reload}
        onRowClick={(row) => setExpandedId((cur) => (cur === row.id ? null : row.id))}
      />

      {expanded ? (
        <Box
          sx={{
            border: 1,
            borderColor: "divider",
            borderRadius: 1,
            p: 2,
            bgcolor: (theme) => theme.palette.action.hover,
          }}
        >
          <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 1.5 }}>
            <HistoryOutlined fontSize="small" color="disabled" />
            <Typography variant="subtitle2">
              {expanded.entityName} · {expanded.entityId} · {expanded.fieldName}
            </Typography>
            <Box sx={{ flex: 1 }} />
            <Typography variant="caption" color="text.secondary" sx={{ fontFamily: "monospace" }}>
              {formatDateTime(expanded.createdAt)}
            </Typography>
          </Stack>
          <Stack spacing={1.5}>
            <Alert type="error">
              <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                {t("audit.oldValue")}
              </Typography>
              <Typography variant="body2" sx={{ fontFamily: "monospace", whiteSpace: "pre-wrap" }}>
                {expanded.oldValue || "—"}
              </Typography>
            </Alert>
            <Alert type="success">
              <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                {t("audit.newValue")}
              </Typography>
              <Typography variant="body2" sx={{ fontFamily: "monospace", whiteSpace: "pre-wrap" }}>
                {expanded.newValue || "—"}
              </Typography>
            </Alert>
          </Stack>
        </Box>
      ) : null}
    </PageStack>
  );
}

export default AuditChanges;
