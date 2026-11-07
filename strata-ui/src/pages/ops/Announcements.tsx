// SPDX-License-Identifier: Apache-2.0
import { Box, DialogActions, Stack, Typography } from "@mui/material";
import {
  Button,
  TextField,
  Select,
  Dialog,
  ConfirmDialog,
  PageHeader,
  DataTable,
  Chip,
  Pagination,
  RichTextEditor,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import {
  opsAnnouncementApi,
  type AnnouncementDto,
  type AnnouncementPayload,
} from "@/shared/api/ops";

const TYPE_COLOR: Record<string, "info" | "warning" | "error" | "default"> = {
  NOTICE: "info",
  BANNER: "warning",
  MAINTENANCE: "error",
};

const STATUS_COLOR: Record<string, "success" | "default"> = {
  PUBLISHED: "success",
  OFFLINE: "default",
};

interface AnnouncementForm {
  title: string;
  content: string;
  type: string;
  startAt: string;
  endAt: string;
  status: string;
  sort: number;
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

export function Announcements() {
  const { t } = useI18n();

  const { rows, total, page, setPage, pageSize, setPageSize, loading, error, reload } =
    usePagedQuery<AnnouncementDto>(["api", "ops", "announcements"], opsAnnouncementApi.page);

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
  } = useResourceCrud<AnnouncementDto, AnnouncementPayload, AnnouncementPayload, AnnouncementForm>({
    create: (payload) => opsAnnouncementApi.create(payload),
    update: (id, payload) => opsAnnouncementApi.update(id, payload),
    remove: (id) => opsAnnouncementApi.remove(id),
    reload,
    initForm: () => ({
      title: "",
      content: "",
      type: "NOTICE",
      startAt: "",
      endAt: "",
      status: "PUBLISHED",
      sort: 0,
    }),
    toEditForm: (r) => ({
      title: r.title,
      content: r.content ?? "",
      type: r.type,
      startAt: toLocalInput(r.startAt),
      endAt: toLocalInput(r.endAt),
      status: r.status,
      sort: r.sort,
    }),
    toCreatePayload: (f) => ({
      title: f.title,
      content: f.content,
      type: f.type,
      startAt: fromLocalInput(f.startAt),
      endAt: fromLocalInput(f.endAt),
      status: f.status,
      sort: f.sort,
    }),
    toUpdatePayload: (f) => ({
      title: f.title,
      content: f.content,
      type: f.type,
      startAt: fromLocalInput(f.startAt),
      endAt: fromLocalInput(f.endAt),
      status: f.status,
      sort: f.sort,
    }),
  });

  const cols = [
    { key: "title", title: t("ops.announcements.title") },
    {
      key: "type",
      title: t("common.type"),
      render: (r: AnnouncementDto) => (
        <Chip
          label={r.type}
          size="small"
          color={TYPE_COLOR[r.type] ?? "default"}
          variant="outlined"
        />
      ),
    },
    {
      key: "status",
      title: t("common.status"),
      render: (r: AnnouncementDto) => (
        <Chip
          label={r.status}
          size="small"
          color={STATUS_COLOR[r.status] ?? "default"}
          variant="outlined"
        />
      ),
    },
    {
      key: "startAt",
      title: t("ops.announcements.startAt"),
      render: (r: AnnouncementDto) => (
        <Typography variant="caption" color="text.secondary">
          {r.startAt ? new Date(r.startAt).toLocaleString() : "—"}
        </Typography>
      ),
    },
    {
      key: "endAt",
      title: t("ops.announcements.endAt"),
      render: (r: AnnouncementDto) => (
        <Typography variant="caption" color="text.secondary">
          {r.endAt ? new Date(r.endAt).toLocaleString() : "—"}
        </Typography>
      ),
    },
    { key: "sort", title: t("common.sort"), width: 80 },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: AnnouncementDto) => (
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
        title={t("ops.announcements.title")}
        subtitle={t("ops.announcements.subtitle")}
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
        tableId="ops-announcements"
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
            label={t("ops.announcements.title")}
            value={form.title}
            onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
          />
          <Box>
            <Typography variant="body2" sx={{ mb: 0.5, color: "text.secondary" }}>
              {t("common.content")}
            </Typography>
            <RichTextEditor
              value={form.content}
              onChange={(html) => setForm((f) => ({ ...f, content: html }))}
              minHeight={220}
            />
          </Box>
          <Select
            label={t("common.type")}
            value={form.type}
            onChange={(e) => setForm((f) => ({ ...f, type: e.target.value }))}
            options={[
              { value: "NOTICE", label: t("ops.announcements.typeNotice") },
              { value: "BANNER", label: t("ops.announcements.typeBanner") },
              { value: "MAINTENANCE", label: t("ops.announcements.typeMaintenance") },
            ]}
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
            label={t("common.status")}
            value={form.status}
            onChange={(e) => setForm((f) => ({ ...f, status: e.target.value }))}
            options={[
              { value: "PUBLISHED", label: t("ops.announcements.statusPublished") },
              { value: "OFFLINE", label: t("ops.announcements.statusOffline") },
            ]}
          />
          <TextField
            label={t("common.sort")}
            type="number"
            value={form.sort}
            onChange={(e) => setForm((f) => ({ ...f, sort: Number(e.target.value) }))}
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
        description={t("common.deleteNamed").replace("{name}", deleting?.title ?? "")}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default Announcements;
