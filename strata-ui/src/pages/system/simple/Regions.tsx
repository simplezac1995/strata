// SPDX-License-Identifier: Apache-2.0
import { Box, DialogActions, Stack } from "@mui/material";
import {
  Button,
  TextField,
  Dialog,
  ConfirmDialog,
  PageHeader,
  DataTable,
  Pagination,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { regionApi, type RegionPayload, type RegionUpdatePayload } from "@/shared/api/system";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import type { RegionDto } from "@/shared/types";

export function Regions() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("system:region:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("system:region:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:region:delete"));

  const {
    rows: regions,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    error,
    reload,
  } = usePagedQuery<RegionDto>(["api", "system", "regions"], regionApi.page);

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
    RegionDto,
    RegionPayload,
    RegionUpdatePayload,
    { code: string; name: string; level: number; parentId: string }
  >({
    create: (payload) => regionApi.create(payload),
    update: (id, payload) => regionApi.update(id, payload),
    remove: (id) => regionApi.remove(id),
    reload,
    initForm: () => ({ code: "", name: "", level: 1, parentId: "" }),
    toEditForm: (r) => ({
      code: r.code,
      name: r.name,
      level: r.level,
      parentId: r.parentId ?? "",
    }),
    toCreatePayload: (f) => ({
      code: f.code,
      name: f.name,
      level: f.level,
      parentId: f.parentId.trim() ? f.parentId : null,
    }),
    toUpdatePayload: (f) => ({
      name: f.name,
      level: f.level,
      parentId: f.parentId.trim() ? f.parentId : null,
    }),
  });

  const cols = [
    { key: "id", title: t("common.id"), width: 64 },
    {
      key: "code",
      title: t("common.code"),
      render: (r: RegionDto) => (
        <Box component="code" sx={{ fontSize: 12, fontFamily: "monospace" }}>
          {r.code}
        </Box>
      ),
    },
    { key: "name", title: t("common.name") },
    { key: "level", title: t("common.level") },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: RegionDto) => (
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
        title={t("nav.regions")}
        actions={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              {t("system.regions.addRegion")}
            </Button>
          ) : undefined
        }
      />
      <DataTable
        columns={cols}
        data={regions}
        loading={loading}
        rowKey={(r) => r.id}
        tableId="regions"
        error={error}
        onRetry={reload}
        emptyAction={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              {t("system.regions.addRegion")}
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
        title={t(edit ? "system.regions.editRegion" : "system.regions.createRegion")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("common.codeRequired")}
            placeholder="CN"
            value={form.code}
            onChange={(e) => setForm((f) => ({ ...f, code: e.target.value }))}
            disabled={!!edit}
          />
          <TextField
            label={t("common.nameRequired")}
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
          />
          <TextField
            label={t("common.level")}
            type="number"
            value={String(form.level)}
            onChange={(e) => setForm((f) => ({ ...f, level: Number(e.target.value) }))}
          />
          <TextField
            label={t("system.regions.parentId")}
            placeholder={t("common.optional")}
            value={form.parentId}
            onChange={(e) => setForm((f) => ({ ...f, parentId: e.target.value }))}
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

export default Regions;
