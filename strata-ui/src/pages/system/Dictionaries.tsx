// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, DialogActions, Grid, Stack, Typography } from "@mui/material";
import {
  Button,
  TextField,
  Select,
  Dialog,
  ConfirmDialog,
  PageHeader,
  DataTable,
} from "@/components/ui/index";
import { ContentCard, PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import {
  dictDataApi,
  dictTypeApi,
  type DictDataCreatePayload,
  type DictDataUpdatePayload,
  type DictTypeCreatePayload,
  type DictTypeUpdatePayload,
} from "@/shared/api/system";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import type { DictDataDto, DictTypeDto } from "@/shared/types";

export function Dictionaries() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("system:dict:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("system:dict:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:dict:delete"));

  const {
    data: types,
    loading,
    reload,
  } = useListQuery<DictTypeDto>(["api", "system", "dict-types"], dictTypeApi.list, {
    staleTime: 5 * 60 * 1000,
  });
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const selected = types.find((x) => x.id === selectedId) ?? types[0] ?? null;

  const {
    data: items,
    loading: itemsLoading,
    reload: reloadItems,
  } = useListQuery<DictDataDto>(["api", "system", "dict-data", { typeId: selected?.id }], () =>
    selected ? dictDataApi.listByType(selected.id) : Promise.resolve([]),
  );

  const typeCrud = useResourceCrud<
    DictTypeDto,
    DictTypeCreatePayload,
    DictTypeUpdatePayload,
    { code: string; name: string; sortOrder: number; enabled: boolean }
  >({
    create: (payload) => dictTypeApi.create(payload),
    update: (id, payload) => dictTypeApi.update(id, payload),
    remove: (id) => dictTypeApi.remove(id),
    reload,
    initForm: () => ({ code: "", name: "", sortOrder: 1, enabled: true }),
    toEditForm: (dt) => ({
      code: dt.code,
      name: dt.name,
      sortOrder: dt.sortOrder,
      enabled: dt.enabled,
    }),
    toCreatePayload: (f) => ({ ...f }),
    toUpdatePayload: (f) => ({ name: f.name, sortOrder: f.sortOrder, enabled: f.enabled }),
    onDeleted: (dt) => {
      if (selectedId === dt.id) setSelectedId(null);
    },
  });

  const itemCrud = useResourceCrud<
    DictDataDto,
    DictDataCreatePayload,
    DictDataUpdatePayload,
    { value: string; label: string; sortOrder: number; enabled: boolean }
  >({
    create: (payload) => dictDataApi.create(payload),
    update: (id, payload) => dictDataApi.update(id, payload),
    remove: (id) => dictDataApi.remove(id),
    reload: reloadItems,
    initForm: () => ({ value: "", label: "", sortOrder: 1, enabled: true }),
    toEditForm: (di) => ({
      value: di.value,
      label: di.label,
      sortOrder: di.sortOrder,
      enabled: di.enabled,
    }),
    toCreatePayload: (f) => ({
      typeId: selected!.id,
      value: f.value,
      label: f.label,
      sortOrder: f.sortOrder,
      enabled: f.enabled,
    }),
    toUpdatePayload: (f) => ({
      label: f.label,
      sortOrder: f.sortOrder,
      enabled: f.enabled,
    }),
    canSave: () => selected !== null,
  });

  const itemCols = [
    {
      key: "value",
      title: t("common.value"),
      render: (r: DictDataDto) => (
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
          {r.value}
        </Box>
      ),
    },
    { key: "label", title: t("common.label") },
    { key: "sortOrder", title: t("common.sort") },
    {
      key: "enabled",
      title: t("common.status"),
      render: (r: DictDataDto) => (r.enabled ? t("common.enabled") : t("common.disabled")),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: DictDataDto) => (
        <Stack direction="row" spacing={0.5}>
          {canUpdate && (
            <Button variant="text" size="sm" onClick={() => itemCrud.openEdit(r)}>
              {t("common.edit")}
            </Button>
          )}
          {canDelete && (
            <Button
              variant="text"
              size="sm"
              color="danger"
              onClick={() => itemCrud.confirmDelete(r)}
            >
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
        title={t("nav.dictionaries")}
        subtitle={t("system.dictionaries.manageEnumerationDataTypesAndValues")}
      />
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 3 }}>
          <ContentCard padding={false} sx={{ alignSelf: "flex-start" }}>
            <Stack
              direction="row"
              sx={{
                alignItems: "center",
                justifyContent: "space-between",
                px: 2,
                py: 1.5,
                borderBottom: 1,
                borderColor: "divider",
              }}
            >
              <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                {t("system.dictionaries.dictTypes")}
              </Typography>
              {canCreate ? (
                <Button variant="text" size="sm" color="primary" onClick={typeCrud.openCreate}>
                  +
                </Button>
              ) : null}
            </Stack>
            {loading ? (
              <Box sx={{ px: 2, py: 1.5 }}>
                <Typography variant="body2" color="text.secondary">
                  {t("common.loading")}
                </Typography>
              </Box>
            ) : (
              types.map((type) => (
                <Box
                  key={type.id}
                  onClick={() => setSelectedId(type.id)}
                  sx={{
                    px: 2,
                    py: 1.25,
                    cursor: "pointer",
                    typography: "body2",
                    bgcolor: selected?.id === type.id ? "primary.50" : "transparent",
                    color: selected?.id === type.id ? "primary.main" : "text.secondary",
                    "&:hover": {
                      bgcolor: selected?.id === type.id ? "primary.50" : "action.hover",
                    },
                  }}
                >
                  {type.code}
                </Box>
              ))
            )}
          </ContentCard>
        </Grid>

        <Grid size={{ xs: 12, md: 9 }}>
          <Stack
            direction="row"
            sx={{ alignItems: "center", justifyContent: "space-between", mb: 1.5 }}
          >
            <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {selected?.code ?? "—"}
              </Typography>
              {canUpdate && selected ? (
                <Button variant="text" size="sm" onClick={() => typeCrud.openEdit(selected)}>
                  {t("common.edit")}
                </Button>
              ) : null}
              {canDelete && selected ? (
                <Button
                  variant="text"
                  size="sm"
                  color="danger"
                  onClick={() => typeCrud.confirmDelete(selected)}
                >
                  {t("common.delete")}
                </Button>
              ) : null}
            </Stack>
            {canCreate ? (
              <Button
                variant="contained"
                size="sm"
                disabled={!selected}
                onClick={itemCrud.openCreate}
              >
                {t("system.dictionaries.addItem")}
              </Button>
            ) : null}
          </Stack>
          <DataTable columns={itemCols} data={items} loading={itemsLoading} rowKey={(r) => r.id} />
        </Grid>
      </Grid>

      <Dialog
        open={typeCrud.open}
        onClose={typeCrud.closeDialog}
        title={t(
          typeCrud.edit ? "system.dictionaries.editDictType" : "system.dictionaries.createDictType",
        )}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("common.codeRequired")}
            placeholder="user_status"
            value={typeCrud.form.code}
            disabled={!!typeCrud.edit}
            onChange={(e) => typeCrud.setForm((f) => ({ ...f, code: e.target.value }))}
          />
          <TextField
            label={t("common.nameRequired")}
            placeholder={t("system.dictionaries.placeholderDictTypeLabel")}
            value={typeCrud.form.name}
            onChange={(e) => typeCrud.setForm((f) => ({ ...f, name: e.target.value }))}
          />
          <TextField
            label={t("common.sortOrder")}
            type="number"
            placeholder="1"
            value={String(typeCrud.form.sortOrder)}
            onChange={(e) => typeCrud.setForm((f) => ({ ...f, sortOrder: Number(e.target.value) }))}
          />
          <Select
            label={t("common.status")}
            value={typeCrud.form.enabled ? "true" : "false"}
            onChange={(e) =>
              typeCrud.setForm((f) => ({ ...f, enabled: e.target.value === "true" }))
            }
            options={[
              { value: "true", label: t("common.enabled") },
              { value: "false", label: t("common.disabled") },
            ]}
          />
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={typeCrud.closeDialog}>
            {t("common.cancel")}
          </Button>
          <Button
            variant="contained"
            disabled={typeCrud.busy}
            onClick={() => void typeCrud.handleSave()}
          >
            {typeCrud.busy ? t("common.saving") : t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={itemCrud.open}
        onClose={itemCrud.closeDialog}
        title={t(itemCrud.edit ? "system.dictionaries.editItem" : "system.dictionaries.createItem")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("system.common.value")}
            placeholder="pending"
            value={itemCrud.form.value}
            disabled={!!itemCrud.edit}
            onChange={(e) => itemCrud.setForm((f) => ({ ...f, value: e.target.value }))}
          />
          <TextField
            label={t("system.dictionaries.label")}
            placeholder={t("system.dictionaries.pending")}
            value={itemCrud.form.label}
            onChange={(e) => itemCrud.setForm((f) => ({ ...f, label: e.target.value }))}
          />
          <TextField
            label={t("common.sortOrder")}
            type="number"
            placeholder="1"
            value={String(itemCrud.form.sortOrder)}
            onChange={(e) => itemCrud.setForm((f) => ({ ...f, sortOrder: Number(e.target.value) }))}
          />
          <Select
            label={t("common.status")}
            value={itemCrud.form.enabled ? "true" : "false"}
            onChange={(e) =>
              itemCrud.setForm((f) => ({ ...f, enabled: e.target.value === "true" }))
            }
            options={[
              { value: "true", label: t("common.enabled") },
              { value: "false", label: t("common.disabled") },
            ]}
          />
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={itemCrud.closeDialog}>
            {t("common.cancel")}
          </Button>
          <Button
            variant="contained"
            disabled={itemCrud.busy}
            onClick={() => void itemCrud.handleSave()}
          >
            {itemCrud.busy ? t("common.saving") : t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={typeCrud.deleting !== null}
        onClose={typeCrud.closeDelete}
        onConfirm={() => void typeCrud.runDelete()}
        title={t("system.dictionaries.deleteDictType")}
        description={t("system.dicts.delete.withAsterisk").replace(
          "{name}",
          typeCrud.deleting?.code ?? "",
        )}
        confirmColor="danger"
      />

      <ConfirmDialog
        open={itemCrud.deleting !== null}
        onClose={itemCrud.closeDelete}
        onConfirm={() => void itemCrud.runDelete()}
        title={t("system.dictionaries.deleteItem")}
        description={t("system.dictionaries.deleteItemName").replace(
          "{name}",
          itemCrud.deleting?.label ?? "",
        )}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default Dictionaries;
