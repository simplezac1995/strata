// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, DialogActions, Stack, Typography } from "@mui/material";
import { CheckCircleOutlined, CloseOutlined, PersonOutlineOutlined } from "@mui/icons-material";
import {
  Button,
  Chip,
  DataTable,
  Dialog,
  PageHeader,
  Select,
  TextArea,
  TextField,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { complianceApi, type KycApplicationDto, type KycStatus } from "@/shared/api/compliance";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { formatDateTime } from "@/shared/utils/date";

const STATUS_TONE: Record<KycStatus, "warning" | "success" | "error" | "default"> = {
  PENDING: "warning",
  APPROVED: "success",
  REJECTED: "error",
};

const KEY = ["api", "compliance", "kyc"];

/**
 * KYC application review queue.
 *
 * Hidden from the navigation menu by product requirement — the file exists so
 * the route can be wired in lazily later without rework.
 */
export function KycReview() {
  const { t } = useI18n();
  const [status, setStatus] = useState("");

  const { rows, total, page, setPage, pageSize, setPageSize, loading, error, reload } =
    usePagedQuery<KycApplicationDto>([...KEY, { status }], (pageIndex, size) =>
      complianceApi.kycApplicationsPage({ status }, pageIndex, size),
    );

  /* Detail dialog (row click) */
  const [detail, setDetail] = useState<KycApplicationDto | null>(null);
  /* Reject reason dialog */
  const [rejectTarget, setRejectTarget] = useState<KycApplicationDto | null>(null);
  const [reason, setReason] = useState("");

  const approve = useMutationWithToast(
    (row: KycApplicationDto) => complianceApi.kycReview(row.id, { approved: true }),
    { success: "compliance.kyc.approved", failure: "compliance.kyc.reviewFailed" },
    { invalidateKeys: [KEY] },
  );

  const reject = useMutationWithToast(
    ({ row, reasonText }: { row: KycApplicationDto; reasonText: string }) =>
      complianceApi.kycReview(row.id, { approved: false, reason: reasonText }),
    { success: "compliance.kyc.rejected", failure: "compliance.kyc.reviewFailed" },
    { invalidateKeys: [KEY] },
  );

  async function handleReject() {
    if (!rejectTarget || !reason.trim()) return;
    try {
      await reject.mutateAsync({ row: rejectTarget, reasonText: reason.trim() });
      setRejectTarget(null);
      setReason("");
      setDetail(null);
    } catch {
      // toast already shown
    }
  }

  async function handleApprove(row: KycApplicationDto) {
    try {
      await approve.mutateAsync(row);
      setDetail(null);
    } catch {
      // toast already shown
    }
  }

  const columns = [
    {
      key: "userId",
      title: t("compliance.kyc.user"),
      render: (r: KycApplicationDto) => (
        <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }}>
          <PersonOutlineOutlined fontSize="small" color="disabled" />
          <Typography variant="body2">{r.userId || "—"}</Typography>
        </Stack>
      ),
    },
    {
      key: "realName",
      title: t("compliance.kyc.realName"),
      render: (r: KycApplicationDto) => r.realName,
    },
    {
      key: "idType",
      title: t("compliance.kyc.idType"),
      render: (r: KycApplicationDto) => <Chip label={r.idType} color="default" />,
    },
    {
      key: "status",
      title: t("common.status"),
      render: (r: KycApplicationDto) => (
        <Chip
          label={t(`compliance.kyc.status.${r.status}`)}
          color={STATUS_TONE[r.status] ?? "default"}
        />
      ),
    },
    {
      key: "submittedAt",
      title: t("compliance.kyc.submittedAt"),
      render: (r: KycApplicationDto) => (
        <Typography variant="caption" color="text.secondary" sx={{ fontFamily: "monospace" }}>
          {formatDateTime(r.submittedAt)}
        </Typography>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader title={t("compliance.kyc.title")} subtitle={t("compliance.kyc.subtitle")} />

      <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
        <Select
          label={t("common.status")}
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(1);
          }}
          options={[
            { value: "", label: t("common.all") },
            { value: "PENDING", label: t("compliance.kyc.status.PENDING") },
            { value: "APPROVED", label: t("compliance.kyc.status.APPROVED") },
            { value: "REJECTED", label: t("compliance.kyc.status.REJECTED") },
          ]}
          sx={{ minWidth: 200 }}
        />
      </Stack>

      <DataTable
        columns={columns}
        data={rows}
        loading={loading}
        rowKey={(r) => r.id}
        total={total}
        page={page}
        pageSize={pageSize}
        onPageChange={setPage}
        onPageSizeChange={setPageSize}
        tableId="compliance.kyc"
        error={error}
        onRetry={reload}
        onRowClick={(r) => setDetail(r)}
      />

      {/* Detail dialog (row click) */}
      <Dialog
        open={!!detail}
        onClose={() => setDetail(null)}
        title={t("compliance.kyc.detailTitle")}
        maxWidth="sm"
      >
        {detail ? (
          <Stack spacing={2} sx={{ py: 1 }}>
            <TextField
              label={t("compliance.kyc.user")}
              value={detail.userId}
              slotProps={{ input: { readOnly: true } }}
            />
            <TextField
              label={t("compliance.kyc.realName")}
              value={detail.realName}
              slotProps={{ input: { readOnly: true } }}
            />
            <TextField
              label={t("compliance.kyc.idType")}
              value={detail.idType}
              slotProps={{ input: { readOnly: true } }}
            />
            <TextField
              label={t("compliance.kyc.idNumber")}
              value={detail.idNumber}
              slotProps={{ input: { readOnly: true } }}
            />
            <Box>
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{ display: "block", mb: 0.5 }}
              >
                {t("compliance.kyc.idImage")}
              </Typography>
              {detail.idImageUrl ? (
                <Box
                  component="img"
                  src={detail.idImageUrl}
                  alt={t("compliance.kyc.idImage")}
                  sx={{
                    width: "100%",
                    maxHeight: 240,
                    objectFit: "contain",
                    borderRadius: 1,
                    border: 1,
                    borderColor: "divider",
                  }}
                />
              ) : (
                <Typography variant="body2" color="text.secondary">
                  —
                </Typography>
              )}
            </Box>
            {detail.reason ? (
              <TextField
                label={t("compliance.kyc.reviewReason")}
                value={detail.reason}
                slotProps={{ input: { readOnly: true } }}
              />
            ) : null}
          </Stack>
        ) : null}
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={() => setDetail(null)}>
            {t("common.cancel")}
          </Button>
          {detail?.status === "PENDING" ? (
            <>
              <Button
                variant="outlined"
                color="danger"
                disabled={approve.isPending || reject.isPending}
                onClick={() => {
                  setRejectTarget(detail);
                  setReason("");
                }}
              >
                <CloseOutlined fontSize="small" sx={{ mr: 0.5 }} />
                {t("compliance.kyc.reject")}
              </Button>
              <Button
                variant="contained"
                disabled={approve.isPending || reject.isPending}
                onClick={() => detail && void handleApprove(detail)}
              >
                <CheckCircleOutlined fontSize="small" sx={{ mr: 0.5 }} />
                {t("compliance.kyc.approve")}
              </Button>
            </>
          ) : null}
        </DialogActions>
      </Dialog>

      {/* Reject reason dialog */}
      <Dialog
        open={!!rejectTarget}
        onClose={() => setRejectTarget(null)}
        title={t("compliance.kyc.rejectTitle")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextArea
            label={t("compliance.kyc.rejectReason")}
            placeholder={t("compliance.kyc.rejectReasonPlaceholder")}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={3}
          />
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={() => setRejectTarget(null)}>
            {t("common.cancel")}
          </Button>
          <Button
            variant="contained"
            color="danger"
            disabled={!reason.trim() || reject.isPending}
            onClick={() => void handleReject()}
          >
            {reject.isPending ? t("common.saving") : t("compliance.kyc.reject")}
          </Button>
        </DialogActions>
      </Dialog>
    </PageStack>
  );
}

export default KycReview;
