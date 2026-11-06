// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, Chip, DialogActions, Stack, Typography } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import {
  Button,
  PageHeader,
  DataTable,
  Dialog,
  ConfirmDialog,
  TextField,
  Switch,
  Pagination,
} from "@/components/ui/index";
import { InfoBanner } from "@/shared/pageKit/InfoBanner";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { jobApi, platformCapabilitiesApi, type JobCreatePayload } from "@/shared/api/system";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import type { JobDto } from "@/shared/types";

const STATUS_COLOR: Record<string, "success" | "default" | "error"> = {
  RUNNING: "success",
  PAUSED: "default",
  ERROR: "error",
};

function formatInstant(value: string | null): string {
  if (!value) return "—";
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value;
  }
}

export function Schedules() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("system:job:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("system:job:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:job:delete"));

  const {
    rows: jobs,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    reload,
  } = usePagedQuery<JobDto>(["api", "system", "jobs"], jobApi.page);

  const { data: capabilities } = useQuery({
    queryKey: ["api", "system", "platform-capabilities"],
    queryFn: () => platformCapabilitiesApi.get(),
  });
  const jobStub = capabilities?.jobSchedulerStub ?? true;

  const [open, setOpen] = useState(false);
  const [edit, setEdit] = useState<JobDto | null>(null);
  const [deleting, setDeleting] = useState<JobDto | null>(null);
  const [form, setForm] = useState<JobCreatePayload>({
    name: "",
    jobGroup: "system",
    cron: "0 0 * * * ?",
    enabled: true,
  });

  const createMutation = useMutationWithToast(
    (payload: JobCreatePayload) => jobApi.create(payload),
    { success: "toast.saved", failure: "toast.saveFailed" },
    { onSuccess: () => reload() },
  );

  const updateMutation = useMutationWithToast(
    ({ id, payload }: { id: string; payload: JobCreatePayload }) => jobApi.update(id, payload),
    { success: "toast.saved", failure: "toast.saveFailed" },
    { onSuccess: () => reload() },
  );

  const pauseMutation = useMutationWithToast(
    (id: string) => jobApi.pause(id),
    { success: "toast.saved", failure: "toast.saveFailed" },
    { onSuccess: () => reload() },
  );

  const runMutation = useMutationWithToast(
    (id: string) => jobApi.run(id),
    { success: "toast.saved", failure: "toast.saveFailed" },
    { onSuccess: () => reload() },
  );

  const deleteMutation = useMutationWithToast(
    (id: string) => jobApi.remove(id),
    { success: "toast.deleted", failure: "toast.deleteFailed" },
    { onSuccess: () => reload() },
  );

  function openCreate() {
    setEdit(null);
    setForm({ name: "", jobGroup: "system", cron: "0 0 * * * ?", enabled: true });
    setOpen(true);
  }

  function openEdit(row: JobDto) {
    setEdit(row);
    setForm({
      name: row.name,
      jobGroup: row.jobGroup,
      cron: row.cron,
      enabled: row.enabled,
    });
    setOpen(true);
  }

  async function handleSave() {
    try {
      if (edit) {
        await updateMutation.mutateAsync({ id: edit.id, payload: form });
      } else {
        await createMutation.mutateAsync(form);
      }
      setOpen(false);
    } catch {
      // toast already shown
    }
  }

  const cols = [
    { key: "name", title: t("tools.schedules.jobName") },
    {
      key: "jobGroup",
      title: t("tools.schedules.group"),
      render: (row: JobDto) => (
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
          {row.jobGroup}
        </Box>
      ),
    },
    {
      key: "cron",
      title: t("tools.schedules.cron"),
      render: (row: JobDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.secondary">
          {row.cron}
        </Typography>
      ),
    },
    {
      key: "status",
      title: t("common.status"),
      render: (row: JobDto) => (
        <Chip
          label={row.status}
          size="small"
          color={STATUS_COLOR[row.status] ?? "default"}
          variant="outlined"
        />
      ),
    },
    {
      key: "lastRun",
      title: t("tools.schedules.lastRun"),
      render: (row: JobDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.disabled">
          {formatInstant(row.lastRun)}
        </Typography>
      ),
    },
    {
      key: "nextRun",
      title: t("tools.schedules.nextRun"),
      render: (row: JobDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.disabled">
          {formatInstant(row.nextRun)}
        </Typography>
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (row: JobDto) => (
        <Stack direction="row" spacing={0.5}>
          {canUpdate && (
            <Button
              variant="text"
              size="sm"
              color="primary"
              title={t("tools.schedules.jobRunSimulated")}
              onClick={() => void runMutation.mutateAsync(row.id).catch(() => {})}
            >
              {t("tools.schedules.run")}
            </Button>
          )}
          {canUpdate && row.status === "RUNNING" && (
            <Button
              variant="text"
              size="sm"
              onClick={() => void pauseMutation.mutateAsync(row.id).catch(() => {})}
            >
              {t("tools.schedules.pause")}
            </Button>
          )}
          {canUpdate && (
            <Button variant="text" size="sm" onClick={() => openEdit(row)}>
              {t("common.edit")}
            </Button>
          )}
          {canDelete && (
            <Button variant="text" size="sm" color="danger" onClick={() => setDeleting(row)}>
              {t("common.delete")}
            </Button>
          )}
        </Stack>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("tools.schedules.schedules")}
        subtitle={`${t("tools.schedules.cronJobManagement")} · ${t("tools.schedules.jobRunSimulated")}`}
        actions={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              {t("tools.schedules.createJob")}
            </Button>
          ) : undefined
        }
      />
      {jobStub ? <InfoBanner>{t("tools.schedules.jobRunSimulated")}</InfoBanner> : null}
      <DataTable columns={cols} data={jobs} loading={loading} rowKey={(row) => row.id} />
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

      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title={edit ? t("tools.schedules.editJob") : t("tools.schedules.createJob")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("tools.schedules.jobName")}
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
          />
          <TextField
            label={t("tools.schedules.group")}
            value={form.jobGroup ?? ""}
            onChange={(e) => setForm((f) => ({ ...f, jobGroup: e.target.value }))}
          />
          <TextField
            label={t("tools.schedules.cron")}
            value={form.cron}
            onChange={(e) => setForm((f) => ({ ...f, cron: e.target.value }))}
          />
          <Switch
            label={t("common.enabled")}
            checked={form.enabled ?? true}
            onChange={(v) => setForm((f) => ({ ...f, enabled: v }))}
          />
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={() => setOpen(false)}>
            {t("common.cancel")}
          </Button>
          <Button variant="contained" onClick={() => void handleSave()}>
            {t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={deleting !== null}
        onClose={() => setDeleting(null)}
        onConfirm={async () => {
          if (!deleting) return;
          try {
            await deleteMutation.mutateAsync(deleting.id);
            setDeleting(null);
          } catch {
            // toast already shown
          }
        }}
        title={t("common.delete")}
        description={t("common.deleteNamed").replace("{name}", deleting?.name ?? "")}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default Schedules;
