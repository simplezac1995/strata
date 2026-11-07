// SPDX-License-Identifier: Apache-2.0
import { Box, DialogActions, Stack } from "@mui/material";
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
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { postApi, type PostPayload } from "@/shared/api/system";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import { exportCsvFromColumns, type CsvColumn } from "@/shared/utils/csv";
import type { PostDto } from "@/shared/types";

export function Posts() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("system:post:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("system:post:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:post:delete"));

  const {
    rows: posts,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    reload,
    error,
  } = usePagedQuery<PostDto>(["api", "system", "posts"], postApi.page);

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
    PostDto,
    PostPayload,
    PostPayload,
    { code: string; name: string; sortOrder: number; enabled: boolean }
  >({
    create: (payload) => postApi.create(payload),
    update: (id, payload) => postApi.update(id, payload),
    remove: (id) => postApi.remove(id),
    reload,
    initForm: () => ({ code: "", name: "", sortOrder: 1, enabled: true }),
    toEditForm: (p) => ({
      code: p.code,
      name: p.name,
      sortOrder: p.sortOrder,
      enabled: p.enabled,
    }),
  });

  const csvColumns: CsvColumn<PostDto>[] = [
    { title: t("system.posts.code"), cell: (p) => p.code },
    { title: t("system.posts.name"), cell: (p) => p.name },
    { title: t("system.posts.sortOrder"), cell: (p) => p.sortOrder },
    {
      title: t("common.status"),
      cell: (p) => (p.enabled ? t("common.enabled") : t("common.disabled")),
    },
  ];

  const cols = [
    {
      key: "code",
      title: t("system.posts.code"),
      render: (p: PostDto) => (
        <Box component="code" sx={{ fontSize: 12, fontFamily: "monospace", color: "primary.main" }}>
          {p.code}
        </Box>
      ),
    },
    { key: "name", title: t("system.posts.name") },
    { key: "sortOrder", title: t("system.posts.sortOrder") },
    {
      key: "enabled",
      title: t("common.status"),
      render: (p: PostDto) => <StatusChip enabled={p.enabled} />,
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (p: PostDto) => (
        <Stack direction="row" spacing={0.5}>
          {canUpdate && (
            <Button variant="text" size="sm" onClick={() => openEdit(p)}>
              {t("common.edit")}
            </Button>
          )}
          {canDelete && (
            <Button variant="text" size="sm" color="danger" onClick={() => confirmDelete(p)}>
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
        title={t("nav.posts")}
        subtitle={t("system.posts.subtitle")}
        actions={
          <Stack direction="row" spacing={1}>
            <ExportCsvButton onClick={() => exportCsvFromColumns("posts.csv", csvColumns, posts)} />
            {canCreate ? (
              <Button variant="contained" onClick={openCreate}>
                {t("system.posts.create")}
              </Button>
            ) : null}
          </Stack>
        }
      />
      <DataTable
        tableId="posts"
        columns={cols}
        data={posts}
        loading={loading}
        rowKey={(p) => p.id}
        error={error}
        onRetry={reload}
        emptyAction={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              {t("system.posts.create")}
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
        title={edit ? t("system.posts.edit") : t("system.posts.create")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("system.posts.code")}
            value={form.code}
            onChange={(e) => setForm((f) => ({ ...f, code: e.target.value }))}
            disabled={!!edit}
          />
          <TextField
            label={t("system.posts.name")}
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
          />
          <TextField
            label={t("system.posts.sortOrder")}
            type="number"
            value={String(form.sortOrder)}
            onChange={(e) => setForm((f) => ({ ...f, sortOrder: Number(e.target.value) }))}
          />
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
        description={t("common.deleteNamed").replace("{name}", deleting?.name ?? "")}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default Posts;
