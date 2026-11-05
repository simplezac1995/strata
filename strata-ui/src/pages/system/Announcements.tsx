// SPDX-License-Identifier: Apache-2.0
import { Box, DialogActions, Stack, Typography } from "@mui/material";
import {
  Button,
  TextField,
  Select,
  Dialog,
  ConfirmDialog,
  ExportCsvButton,
  PageHeader,
  DataTable,
  StatusChip,
  Pagination,
  RichTextEditor,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { announcementApi, type AnnouncementPayload } from "@/shared/api/system";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import { exportCsvFromColumns, type CsvColumn } from "@/shared/utils/csv";
import type { AnnouncementDto } from "@/shared/types";

/** Announcement content is stored as rich-text HTML; the list preview needs plain text. */
function stripHtml(html: string): string {
  return html
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<\/(p|div|li|h[1-6])>/gi, "\n")
    .replace(/<[^>]+>/g, "")
    .replace(/&nbsp;/g, " ")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/\s+\n/g, "\n")
    .trim();
}

export function Announcements() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("system:announcement:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("system:announcement:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:announcement:delete"));

  const {
    rows: announcements,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    error,
    reload,
  } = usePagedQuery<AnnouncementDto>(["api", "system", "announcements"], (pageIndex, size) =>
    announcementApi.page(pageIndex, size),
  );

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
  } = useResourceCrud<
    AnnouncementDto,
    AnnouncementPayload,
    AnnouncementPayload,
    { title: string; content: string; enabled: boolean }
  >({
    create: (payload) => announcementApi.create(payload),
    update: (id, payload) => announcementApi.update(id, payload),
    remove: (id) => announcementApi.remove(id),
    reload,
    initForm: () => ({ title: "", content: "", enabled: true }),
    toEditForm: (r) => ({ title: r.title, content: r.content ?? "", enabled: r.enabled }),
  });

  const csvColumns: CsvColumn<AnnouncementDto>[] = [
    { title: t("system.announcements.title"), cell: (a) => a.title },
    {
      title: t("common.status"),
      cell: (a) => (a.enabled ? t("common.enabled") : t("common.disabled")),
    },
    {
      title: t("common.createdAt"),
      cell: (a) => (a.createdAt ? new Date(a.createdAt).toLocaleString() : ""),
    },
  ];

  const cols = [
    {
      key: "title",
      title: t("system.announcements.title"),
      render: (r: AnnouncementDto) => (
        <Stack>
          <Typography variant="body2" sx={{ fontWeight: 500 }}>
            {r.title}
          </Typography>
          {r.content ? (
            <Typography variant="caption" color="text.secondary" sx={{ mt: 0.25 }}>
              {(() => {
                const plain = stripHtml(r.content);
                return plain.length > 80 ? plain.slice(0, 80) + "..." : plain;
              })()}
            </Typography>
          ) : null}
        </Stack>
      ),
    },
    {
      key: "enabled",
      title: t("common.status"),
      render: (r: AnnouncementDto) => <StatusChip enabled={r.enabled} />,
    },
    {
      key: "createdAt",
      title: t("common.createdAt"),
      render: (r: AnnouncementDto) => (r.createdAt ? new Date(r.createdAt).toLocaleString() : "—"),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: AnnouncementDto) => (
        <Stack direction="row" spacing={0.5}>
          {canUpdate && (
            <Button variant="text" size="sm" onClick={() => openEdit(r)}>
              {t("common.edit")}
            </Button>
          )}
          {canDelete && (
            <Button variant="text" size="sm" color="danger" onClick={() => confirmDelete(r)}>
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
        title={t("nav.announcements")}
        subtitle={t("system.announcements.subtitle")}
        actions={
          <Stack direction="row" spacing={1}>
            <ExportCsvButton
              onClick={() => exportCsvFromColumns("announcements.csv", csvColumns, announcements)}
            />
            {canCreate ? (
              <Button variant="contained" onClick={openCreate}>
                {t("system.announcements.create")}
              </Button>
            ) : null}
          </Stack>
        }
      />
      <DataTable
        columns={cols}
        data={announcements}
        loading={loading}
        rowKey={(r) => r.id}
        tableId="announcements"
        error={error}
        onRetry={reload}
        emptyAction={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              {t("system.announcements.create")}
            </Button>
          ) : undefined
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
        title={edit ? t("system.announcements.edit") : t("system.announcements.create")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("system.announcements.title")}
            value={form.title}
            onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
          />
          <Box>
            <Typography variant="body2" sx={{ mb: 0.5, color: "text.secondary" }}>
              {t("system.announcements.content")}
            </Typography>
            <RichTextEditor
              value={form.content}
              onChange={(html) => setForm((f) => ({ ...f, content: html }))}
              minHeight={220}
            />
          </Box>
          <Select
            label={t("common.status")}
            value={form.enabled ? "true" : "false"}
            onChange={(e) => setForm((f) => ({ ...f, enabled: e.target.value === "true" }))}
            options={[
              { value: "true", label: t("common.enabled") },
              { value: "false", label: t("common.disabled") },
            ]}
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
