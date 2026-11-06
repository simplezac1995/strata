// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, DialogActions, Stack, Typography } from "@mui/material";
import {
  AddCircleOutlineOutlined,
  CheckCircleOutlined,
  CloseOutlined,
  DeleteOutlineOutlined,
  EditOutlined,
  PersonOutlineOutlined,
} from "@mui/icons-material";
import {
  Button,
  Chip,
  ConfirmDialog,
  DataTable,
  Dialog,
  PageHeader,
  Select,
  Switch,
  Tabs,
  TextField,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import {
  complianceApi,
  type AmlReportDto,
  type AmlReportStatus,
  type AmlRuleAction,
  type AmlRuleDto,
  type AmlRulePayload,
} from "@/shared/api/compliance";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { formatDateTime } from "@/shared/utils/date";

type AmlTab = "rules" | "reports";

type RuleForm = {
  name: string;
  type: string;
  threshold: string;
  action: AmlRuleAction;
  enabled: boolean;
};

const ACTION_TONE: Record<AmlRuleAction, "error" | "warning" | "primary" | "default"> = {
  REPORT: "primary",
  BLOCK: "error",
  ALERT: "warning",
};

const REPORT_STATUS_TONE: Record<AmlReportStatus, "warning" | "success" | "error" | "default"> = {
  PENDING: "warning",
  APPROVED: "success",
  REJECTED: "error",
};

const RULES_KEY = ["api", "compliance", "aml", "rules"];
const REPORTS_KEY = ["api", "compliance", "aml", "reports"];

/**
 * AML center: rule CRUD and report triage.
 * Two self-contained tabs; rules are a full list, reports are paged.
 */
export function AmlDashboard() {
  const { t } = useI18n();
  const [tab, setTab] = useState<AmlTab>("rules");
  const [reportStatus, setReportStatus] = useState("");

  const rules = useListQuery<AmlRuleDto>(RULES_KEY, () => complianceApi.amlRulesList());

  const {
    rows: reports,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading: reportsLoading,
    error: reportsError,
    reload: reportsReload,
  } = usePagedQuery<AmlReportDto>([...REPORTS_KEY, { status: reportStatus }], (pageIndex, size) =>
    complianceApi.amlReportsPage({ status: reportStatus }, pageIndex, size),
  );

  /* ── Rules CRUD ── */
  const rulesCrud = useResourceCrud<AmlRuleDto, AmlRulePayload, AmlRulePayload, RuleForm>({
    create: (payload) => complianceApi.amlRuleCreate(payload),
    update: (id, payload) => complianceApi.amlRuleUpdate(id, payload),
    remove: (id) => complianceApi.amlRuleDelete(id),
    reload: rules.reload,
    initForm: () => ({ name: "", type: "", threshold: "1", action: "REPORT", enabled: true }),
    toEditForm: (r) => ({
      name: r.name,
      type: r.type,
      threshold: String(r.threshold),
      action: r.action,
      enabled: r.enabled,
    }),
    toCreatePayload: (form) => ({
      name: form.name,
      type: form.type,
      threshold: Number(form.threshold) || 0,
      action: form.action,
      enabled: form.enabled,
    }),
    toUpdatePayload: (form) => ({
      name: form.name,
      type: form.type,
      threshold: Number(form.threshold) || 0,
      action: form.action,
      enabled: form.enabled,
    }),
  });

  /* ── Report review ── */
  const [reviewTarget, setReviewTarget] = useState<AmlReportDto | null>(null);
  const [note, setNote] = useState("");

  const reviewReport = useMutationWithToast(
    ({ row, approved }: { row: AmlReportDto; approved: boolean }) =>
      complianceApi.amlReportReview(row.id, { approved, note: note.trim() }),
    { success: "compliance.aml.reviewed", failure: "compliance.aml.reviewFailed" },
    {
      invalidateKeys: [REPORTS_KEY],
      onSuccess: () => {
        setReviewTarget(null);
        setNote("");
      },
    },
  );

  const ruleColumns = [
    { key: "name", title: t("compliance.aml.ruleName"), render: (r: AmlRuleDto) => r.name },
    {
      key: "type",
      title: t("common.type"),
      render: (r: AmlRuleDto) => <Chip label={r.type} color="default" />,
    },
    {
      key: "threshold",
      title: t("compliance.aml.threshold"),
      render: (r: AmlRuleDto) => r.threshold,
    },
    {
      key: "action",
      title: t("compliance.aml.action"),
      render: (r: AmlRuleDto) => (
        <Chip
          label={t(`compliance.aml.action.${r.action}`)}
          color={ACTION_TONE[r.action] ?? "default"}
        />
      ),
    },
    {
      key: "enabled",
      title: t("common.status"),
      render: (r: AmlRuleDto) => (
        <Switch
          checked={r.enabled}
          onChange={(checked) =>
            void rulesCrud.updateMutation.mutateAsync({
              id: r.id,
              payload: {
                name: r.name,
                type: r.type,
                threshold: r.threshold,
                action: r.action,
                enabled: checked,
              },
            })
          }
          disabled={rulesCrud.busy}
        />
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: AmlRuleDto) => (
        <Stack direction="row" spacing={0.5}>
          <Button variant="text" size="sm" onClick={() => rulesCrud.openEdit(r)}>
            <EditOutlined fontSize="small" />
          </Button>
          <Button
            variant="text"
            size="sm"
            color="danger"
            onClick={() => rulesCrud.confirmDelete(r)}
          >
            <DeleteOutlineOutlined fontSize="small" />
          </Button>
        </Stack>
      ),
    },
  ];

  const reportColumns = [
    {
      key: "userId",
      title: t("compliance.aml.user"),
      render: (r: AmlReportDto) => (
        <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }}>
          <PersonOutlineOutlined fontSize="small" color="disabled" />
          <Typography variant="body2">{r.userId || "—"}</Typography>
        </Stack>
      ),
    },
    {
      key: "type",
      title: t("common.type"),
      render: (r: AmlReportDto) => <Chip label={r.type} color="primary" />,
    },
    {
      key: "amount",
      title: t("compliance.aml.amount"),
      render: (r: AmlReportDto) => (
        <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
          {r.amount}
        </Typography>
      ),
    },
    {
      key: "description",
      title: t("common.detail"),
      render: (r: AmlReportDto) => (
        <Typography variant="body2" color="text.secondary" noWrap sx={{ maxWidth: 240 }}>
          {r.description || "—"}
        </Typography>
      ),
    },
    {
      key: "status",
      title: t("common.status"),
      render: (r: AmlReportDto) => (
        <Chip
          label={t(`compliance.aml.status.${r.status}`)}
          color={REPORT_STATUS_TONE[r.status] ?? "default"}
        />
      ),
    },
    {
      key: "createdAt",
      title: t("compliance.aml.createdAt"),
      render: (r: AmlReportDto) => (
        <Typography variant="caption" color="text.secondary" sx={{ fontFamily: "monospace" }}>
          {formatDateTime(r.createdAt)}
        </Typography>
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: AmlReportDto) =>
        r.status === "PENDING" ? (
          <Button
            variant="outlined"
            size="sm"
            onClick={() => {
              setReviewTarget(r);
              setNote("");
            }}
          >
            {t("compliance.aml.review")}
          </Button>
        ) : (
          <Typography variant="caption" color="text.secondary">
            —
          </Typography>
        ),
    },
  ];

  return (
    <PageStack>
      <PageHeader title={t("compliance.aml.title")} subtitle={t("compliance.aml.subtitle")} />

      <Tabs
        tabs={[
          { id: "rules", label: t("compliance.aml.tab.rules") },
          { id: "reports", label: t("compliance.aml.tab.reports") },
        ]}
        active={tab}
        onChange={(id) => setTab(id as AmlTab)}
      />

      {tab === "rules" ? (
        <>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
            <Box component="div" sx={{ flex: 1 }} />
            <Button variant="contained" onClick={rulesCrud.openCreate}>
              <AddCircleOutlineOutlined fontSize="small" sx={{ mr: 0.5 }} />
              {t("compliance.aml.ruleCreate")}
            </Button>
          </Stack>
          <DataTable
            columns={ruleColumns}
            data={rules.data}
            loading={rules.loading}
            rowKey={(r) => r.id}
            tableId="compliance.aml.rules"
            onRetry={rules.reload}
          />
        </>
      ) : (
        <>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
            <Select
              label={t("common.status")}
              value={reportStatus}
              onChange={(e) => {
                setReportStatus(e.target.value);
                setPage(1);
              }}
              options={[
                { value: "", label: t("common.all") },
                { value: "PENDING", label: t("compliance.aml.status.PENDING") },
                { value: "APPROVED", label: t("compliance.aml.status.APPROVED") },
                { value: "REJECTED", label: t("compliance.aml.status.REJECTED") },
              ]}
              sx={{ minWidth: 200 }}
            />
          </Stack>
          <DataTable
            columns={reportColumns}
            data={reports}
            loading={reportsLoading}
            rowKey={(r) => r.id}
            total={total}
            page={page}
            pageSize={pageSize}
            onPageChange={setPage}
            onPageSizeChange={setPageSize}
            tableId="compliance.aml.reports"
            error={reportsError}
            onRetry={reportsReload}
          />
        </>
      )}

      {/* Rule create/edit dialog */}
      <Dialog
        open={rulesCrud.open}
        onClose={rulesCrud.closeDialog}
        title={rulesCrud.edit ? t("compliance.aml.ruleEdit") : t("compliance.aml.ruleCreate")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("compliance.aml.ruleName")}
            value={rulesCrud.form.name}
            onChange={(e) => rulesCrud.setForm((f) => ({ ...f, name: e.target.value }))}
          />
          <TextField
            label={t("common.type")}
            value={rulesCrud.form.type}
            onChange={(e) => rulesCrud.setForm((f) => ({ ...f, type: e.target.value }))}
          />
          <TextField
            label={t("compliance.aml.threshold")}
            type="number"
            value={rulesCrud.form.threshold}
            onChange={(e) => rulesCrud.setForm((f) => ({ ...f, threshold: e.target.value }))}
          />
          <Select
            label={t("compliance.aml.action")}
            value={rulesCrud.form.action}
            onChange={(e) =>
              rulesCrud.setForm((f) => ({ ...f, action: e.target.value as AmlRuleAction }))
            }
            options={[
              { value: "REPORT", label: t("compliance.aml.action.REPORT") },
              { value: "BLOCK", label: t("compliance.aml.action.BLOCK") },
              { value: "ALERT", label: t("compliance.aml.action.ALERT") },
            ]}
          />
          <Switch
            checked={rulesCrud.form.enabled}
            onChange={(checked) => rulesCrud.setForm((f) => ({ ...f, enabled: checked }))}
            label={t("common.enabled")}
          />
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={rulesCrud.closeDialog}>
            {t("common.cancel")}
          </Button>
          <Button
            variant="contained"
            disabled={rulesCrud.busy || !rulesCrud.form.name || !rulesCrud.form.type}
            onClick={() => void rulesCrud.handleSave()}
          >
            {rulesCrud.busy ? t("common.saving") : t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Report review dialog */}
      <Dialog
        open={!!reviewTarget}
        onClose={() => setReviewTarget(null)}
        title={t("compliance.aml.reviewTitle")}
      >
        {reviewTarget ? (
          <Stack spacing={2} sx={{ py: 1 }}>
            <TextField
              label={t("compliance.aml.user")}
              value={reviewTarget.userId}
              slotProps={{ input: { readOnly: true } }}
            />
            <TextField
              label={t("compliance.aml.amount")}
              value={String(reviewTarget.amount)}
              slotProps={{ input: { readOnly: true } }}
            />
            <TextField
              label={t("common.detail")}
              value={reviewTarget.description ?? ""}
              slotProps={{ input: { readOnly: true } }}
            />
            <TextField
              label={t("compliance.aml.reviewNote")}
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </Stack>
        ) : null}
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={() => setReviewTarget(null)}>
            {t("common.cancel")}
          </Button>
          <Button
            variant="outlined"
            color="danger"
            disabled={reviewReport.isPending}
            onClick={() =>
              reviewTarget && void reviewReport.mutateAsync({ row: reviewTarget, approved: false })
            }
          >
            <CloseOutlined fontSize="small" sx={{ mr: 0.5 }} />
            {t("compliance.aml.reject")}
          </Button>
          <Button
            variant="contained"
            disabled={reviewReport.isPending}
            onClick={() =>
              reviewTarget && void reviewReport.mutateAsync({ row: reviewTarget, approved: true })
            }
          >
            <CheckCircleOutlined fontSize="small" sx={{ mr: 0.5 }} />
            {t("compliance.aml.approve")}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Rule delete confirmation */}
      <ConfirmDialog
        open={rulesCrud.deleting !== null}
        onClose={rulesCrud.closeDelete}
        onConfirm={() => void rulesCrud.runDelete()}
        title={t("common.delete")}
        description={t("common.deleteNamed").replace("{name}", rulesCrud.deleting?.name ?? "")}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default AmlDashboard;
