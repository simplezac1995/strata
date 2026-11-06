// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, DialogActions, Stack, Typography } from "@mui/material";
import {
  Button,
  TextField,
  Select,
  Dialog,
  ConfirmDialog,
  PageHeader,
  QueryForm,
  QueryField,
  DataTable,
  Pagination,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { paramApi, type ParamCreatePayload, type ParamUpdatePayload } from "@/shared/api/system";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import type { ParamDto } from "@/shared/types";

const PARAM_TYPES = ["string", "number", "boolean", "json"];

export function Parameters() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("system:param:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("system:param:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:param:delete"));

  const [filterKey, setFilterKey] = useState("");

  const {
    rows: params,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    error,
    reload,
  } = usePagedQuery<ParamDto>(["api", "system", "params", { key: filterKey }], (pageIndex, size) =>
    paramApi.page(filterKey, pageIndex, size),
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
    ParamDto,
    ParamCreatePayload,
    ParamUpdatePayload,
    { key: string; value: string; type: string; remark: string }
  >({
    create: (payload) => paramApi.create(payload),
    update: (id, payload) => paramApi.update(id, payload),
    remove: (id) => paramApi.remove(id),
    reload,
    initForm: () => ({ key: "", value: "", type: "string", remark: "" }),
    toEditForm: (p) => ({
      key: p.key,
      value: p.value,
      type: p.type,
      remark: p.remark ?? "",
    }),
    toCreatePayload: (f) => ({
      key: f.key,
      value: f.value,
      type: f.type,
      remark: f.remark || null,
    }),
    toUpdatePayload: (f) => ({
      value: f.value,
      type: f.type,
      remark: f.remark || null,
    }),
  });

  const cols = [
    {
      key: "key",
      title: t("system.parameters.key"),
      render: (p: ParamDto) => (
        <Box component="code" sx={{ fontSize: 12, fontFamily: "monospace", color: "primary.main" }}>
          {p.key}
        </Box>
      ),
    },
    {
      key: "value",
      title: t("system.parameters.value"),
      render: (p: ParamDto) => (
        <Typography variant="body2" sx={{ fontFamily: "monospace", fontSize: 12 }} noWrap>
          {p.value}
        </Typography>
      ),
    },
    {
      key: "type",
      title: t("system.parameters.type"),
      render: (p: ParamDto) => (
        <Box
          component="code"
          sx={{ fontSize: 11, fontFamily: "monospace", color: "text.secondary" }}
        >
          {p.type}
        </Box>
      ),
    },
    {
      key: "remark",
      title: t("system.parameters.remark"),
      render: (p: ParamDto) => (
        <Typography variant="caption" color="text.secondary">
          {p.remark ?? "—"}
        </Typography>
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (p: ParamDto) => (
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
        title={t("nav.parameters")}
        subtitle={t("system.parameters.subtitle")}
        actions={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              {t("system.parameters.create")}
            </Button>
          ) : undefined
        }
      />

      <QueryForm
        onSearch={() => setPage(1)}
        onReset={() => {
          setFilterKey("");
          setPage(1);
        }}
      >
        <QueryField width={240}>
          <TextField
            label={t("system.parameters.key")}
            value={filterKey}
            onChange={(e) => setFilterKey(e.target.value)}
            placeholder={t("system.parameters.keyPlaceholder")}
          />
        </QueryField>
      </QueryForm>

      <DataTable
        columns={cols}
        data={params}
        loading={loading}
        rowKey={(p) => p.id}
        tableId="parameters"
        error={error}
        onRetry={reload}
        emptyAction={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              {t("system.parameters.create")}
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
        title={edit ? t("system.parameters.edit") : t("system.parameters.create")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("system.parameters.key")}
            value={form.key}
            onChange={(e) => setForm((f) => ({ ...f, key: e.target.value }))}
            disabled={!!edit}
          />
          <TextField
            label={t("system.parameters.value")}
            value={form.value}
            onChange={(e) => setForm((f) => ({ ...f, value: e.target.value }))}
          />
          <Select
            label={t("system.parameters.type")}
            value={form.type}
            onChange={(e) => setForm((f) => ({ ...f, type: e.target.value }))}
            options={PARAM_TYPES.map((type) => ({ value: type, label: type }))}
          />
          <TextField
            label={t("system.parameters.remark")}
            value={form.remark}
            onChange={(e) => setForm((f) => ({ ...f, remark: e.target.value }))}
            placeholder={t("common.optional")}
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
        description={t("common.deleteNamed").replace("{name}", deleting?.key ?? "")}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default Parameters;
