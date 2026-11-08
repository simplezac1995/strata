// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Stack, Typography } from "@mui/material";
import {
  Button,
  PageHeader,
  DataTable,
  Chip,
  ConfirmDialog,
  Pagination,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { opsBackupApi, type BackupDto } from "@/shared/api/ops";

const STATUS_COLOR: Record<string, "success" | "default" | "warning" | "error"> = {
  COMPLETED: "success",
  RUNNING: "warning",
  FAILED: "error",
};

function formatSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return "—";
  if (bytes >= 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
  if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${Math.round(bytes / 1024)} KB`;
}

export function Backups() {
  const { t } = useI18n();
  const [restoring, setRestoring] = useState<BackupDto | null>(null);

  const { rows, total, page, setPage, pageSize, setPageSize, loading, error, reload } =
    usePagedQuery<BackupDto>(["api", "ops", "backups"], opsBackupApi.page);

  const createMutation = useMutationWithToast(
    () => opsBackupApi.create(),
    { success: "ops.backups.created", failure: "toast.saveFailed" },
    { onSuccess: () => reload() },
  );

  const restoreMutation = useMutationWithToast(
    (id: string) => opsBackupApi.restore(id),
    { success: "ops.backups.restored", failure: "ops.backups.restoreFailed" },
    {
      onSuccess: () => reload(),
      onSettled: () => setRestoring(null),
    },
  );

  const cols = [
    {
      key: "fileName",
      title: t("ops.backups.fileName"),
      render: (r: BackupDto) => (
        <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
          {r.fileName}
        </Typography>
      ),
    },
    {
      key: "size",
      title: t("common.size"),
      render: (r: BackupDto) => formatSize(r.size),
    },
    { key: "type", title: t("common.type"), render: (r: BackupDto) => r.type },
    {
      key: "status",
      title: t("common.status"),
      render: (r: BackupDto) => (
        <Chip
          label={r.status}
          size="small"
          color={STATUS_COLOR[r.status] ?? "default"}
          variant="outlined"
        />
      ),
    },
    {
      key: "createdAt",
      title: t("common.createdAt"),
      render: (r: BackupDto) => (
        <Typography variant="caption" color="text.secondary">
          {r.createdAt ? new Date(r.createdAt).toLocaleString() : "—"}
        </Typography>
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: BackupDto) => (
        <Stack direction="row" spacing={0.5}>
          <Button variant="text" size="sm" color="warning" onClick={() => setRestoring(r)}>
            {t("ops.backups.restore")}
          </Button>
        </Stack>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("ops.backups.title")}
        subtitle={t("ops.backups.subtitle")}
        actions={
          <Button
            variant="contained"
            disabled={createMutation.isPending}
            onClick={() => void createMutation.mutateAsync()}
          >
            {createMutation.isPending ? t("common.loading") : t("ops.backups.create")}
          </Button>
        }
      />
      <DataTable
        columns={cols}
        data={rows}
        loading={loading}
        rowKey={(r) => r.id}
        tableId="ops-backups"
        error={error}
        onRetry={reload}
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

      <ConfirmDialog
        open={restoring !== null}
        onClose={() => setRestoring(null)}
        onConfirm={() => void restoreMutation.mutateAsync(restoring!.id)}
        title={t("ops.backups.restore")}
        description={t("ops.backups.restoreConfirm").replace("{name}", restoring?.fileName ?? "")}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default Backups;
