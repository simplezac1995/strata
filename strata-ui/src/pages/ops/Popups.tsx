// SPDX-License-Identifier: Apache-2.0
import { Box, DialogActions, Stack, Typography } from "@mui/material";
import {
  Button,
  TextField,
  Select,
  Switch,
  Dialog,
  ConfirmDialog,
  PageHeader,
  DataTable,
  Pagination,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { opsPopupApi, type PopupDto, type PopupPayload } from "@/shared/api/ops";

interface PopupForm {
  name: string;
  imageUrl: string;
  linkUrl: string;
  startAt: string;
  endAt: string;
  frequency: string;
  enabled: boolean;
}

function toLocalInput(value: string | null): string {
  if (!value) return "";
  try {
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return "";
    const pad = (n: number) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  } catch {
    return "";
  }
}

function fromLocalInput(value: string): string | null {
  return value ? value : null;
}

export function Popups() {
  const { t } = useI18n();

  const { rows, total, page, setPage, pageSize, setPageSize, loading, error, reload } =
    usePagedQuery<PopupDto>(["api", "ops", "popups"], opsPopupApi.page);

  const {
    busy,
    open,
    edit,
    deleting,
    form,
    setForm,
    openCreate,
    openEdit,
    closeDialog,
    handleSave,
    confirmDelete,
    closeDelete,
    runDelete,
  } = useResourceCrud<PopupDto, PopupPayload, PopupPayload, PopupForm>({
    create: (payload) => opsPopupApi.create(payload),
    update: (id, payload) => opsPopupApi.update(id, payload),
    remove: (id) => opsPopupApi.remove(id),
    reload,
    initForm: () => ({
      name: "",
      imageUrl: "",
      linkUrl: "",
      startAt: "",
      endAt: "",
      frequency: "ONCE_PER_DAY",
      enabled: true,
    }),
    toEditForm: (r) => ({
      name: r.name,
      imageUrl: r.imageUrl ?? "",
      linkUrl: r.linkUrl ?? "",
      startAt: toLocalInput(r.startAt),
      endAt: toLocalInput(r.endAt),
      frequency: r.frequency,
      enabled: r.enabled,
    }),
    toCreatePayload: (f) => ({
      name: f.name,
      imageUrl: f.imageUrl || null,
      linkUrl: f.linkUrl || null,
      startAt: fromLocalInput(f.startAt),
      endAt: fromLocalInput(f.endAt),
      frequency: f.frequency,
      enabled: f.enabled,
    }),
    toUpdatePayload: (f) => ({
      name: f.name,
      imageUrl: f.imageUrl || null,
      linkUrl: f.linkUrl || null,
      startAt: fromLocalInput(f.startAt),
      endAt: fromLocalInput(f.endAt),
      frequency: f.frequency,
      enabled: f.enabled,
    }),
  });

  const toggleMutation = useMutationWithToast(
    ({ row, enabled }: { row: PopupDto; enabled: boolean }) =>
      opsPopupApi.update(row.id, {
        name: row.name,
        imageUrl: row.imageUrl,
        linkUrl: row.linkUrl,
        startAt: row.startAt,
        endAt: row.endAt,
        frequency: row.frequency,
        enabled,
      }),
    { success: "toast.saved", failure: "toast.saveFailed" },
    { onSuccess: () => reload() },
  );

  const cols = [
    { key: "name", title: t("common.name") },
    {
      key: "imageUrl",
      title: t("ops.popups.image"),
      width: 90,
      render: (r: PopupDto) =>
        r.imageUrl ? (
          <Box
            component="img"
            src={r.imageUrl}
            alt={r.name}
            sx={{ width: 64, height: 40, objectFit: "cover", borderRadius: 1 }}
          />
        ) : (
          "—"
        ),
    },
    {
      key: "linkUrl",
      title: t("ops.popups.link"),
      render: (r: PopupDto) =>
        r.linkUrl ? (
          <Typography variant="caption" color="primary" sx={{ wordBreak: "break-all" }}>
            {r.linkUrl}
          </Typography>
        ) : (
          "—"
        ),
    },
    {
      key: "startAt",
      title: t("ops.announcements.startAt"),
      render: (r: PopupDto) => (
        <Typography variant="caption" color="text.secondary">
          {r.startAt ? new Date(r.startAt).toLocaleString() : "—"}
        </Typography>
      ),
    },
    {
      key: "endAt",
      title: t("ops.announcements.endAt"),
      render: (r: PopupDto) => (
        <Typography variant="caption" color="text.secondary">
          {r.endAt ? new Date(r.endAt).toLocaleString() : "—"}
        </Typography>
      ),
    },
    { key: "frequency", title: t("ops.popups.frequency") },
    {
      key: "enabled",
      title: t("common.enabled"),
      width: 90,
      render: (r: PopupDto) => (
        <Switch
          checked={r.enabled}
          disabled={toggleMutation.isPending}
          onChange={(v) => void toggleMutation.mutateAsync({ row: r, enabled: v })}
        />
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: PopupDto) => (
        <Stack direction="row" spacing={0.5}>
          <Button variant="text" size="sm" onClick={() => openEdit(r)}>
            {t("common.edit")}
          </Button>
          <Button variant="text" size="sm" color="danger" onClick={() => confirmDelete(r)}>
            {t("common.delete")}
          </Button>
        </Stack>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("ops.popups.title")}
        subtitle={t("ops.popups.subtitle")}
        actions={
          <Button variant="contained" onClick={openCreate}>
            {t("common.create")}
          </Button>
        }
      />
      <DataTable
        columns={cols}
        data={rows}
        loading={loading}
        rowKey={(r) => r.id}
        tableId="ops-popups"
        error={error}
        onRetry={reload}
        emptyAction={
          <Button variant="contained" onClick={openCreate}>
            {t("common.create")}
          </Button>
        }
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

      <Dialog
        open={open}
        onClose={closeDialog}
        title={edit ? t("common.edit") : t("common.create")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("common.name")}
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
          />
          <TextField
            label={t("ops.popups.imageUrl")}
            value={form.imageUrl}
            onChange={(e) => setForm((f) => ({ ...f, imageUrl: e.target.value }))}
          />
          <TextField
            label={t("ops.popups.linkUrl")}
            value={form.linkUrl}
            onChange={(e) => setForm((f) => ({ ...f, linkUrl: e.target.value }))}
          />
          <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 2 }}>
            <TextField
              label={t("ops.announcements.startAt")}
              type="datetime-local"
              value={form.startAt}
              onChange={(e) => setForm((f) => ({ ...f, startAt: e.target.value }))}
              slotProps={{ inputLabel: { shrink: true } }}
            />
            <TextField
              label={t("ops.announcements.endAt")}
              type="datetime-local"
              value={form.endAt}
              onChange={(e) => setForm((f) => ({ ...f, endAt: e.target.value }))}
              slotProps={{ inputLabel: { shrink: true } }}
            />
          </Box>
          <Select
            label={t("ops.popups.frequency")}
            value={form.frequency}
            onChange={(e) => setForm((f) => ({ ...f, frequency: e.target.value }))}
            options={[
              { value: "ONCE_PER_DAY", label: t("ops.popups.freqDaily") },
              { value: "ONCE_PER_SESSION", label: t("ops.popups.freqSession") },
              { value: "ALWAYS", label: t("ops.popups.freqAlways") },
            ]}
          />
          <Switch
            label={t("common.enabled")}
            checked={form.enabled}
            onChange={(v) => setForm((f) => ({ ...f, enabled: v }))}
          />
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={closeDialog}>
            {t("common.cancel")}
          </Button>
          <Button variant="contained" disabled={busy} onClick={() => void handleSave()}>
            {busy ? t("common.saving") : t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={deleting !== null}
        onClose={closeDelete}
        onConfirm={() => void runDelete()}
        title={t("common.delete")}
        description={t("common.deleteNamed").replace("{name}", deleting?.name ?? "")}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default Popups;
