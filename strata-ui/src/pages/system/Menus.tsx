// SPDX-License-Identifier: Apache-2.0
import {
  DialogActions,
  Grid,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import {
  Button,
  Dialog,
  ConfirmDialog,
  PageHeader,
  TextField,
  Select,
  Chip,
  QueryForm,
  DataTable,
  Pagination,
} from "@/components/ui/index";
import { ContentCard, PageStack } from "@/shared/pageKit/PageLayout";
import type { MenuDto, MenuType } from "@/shared/types";
import { typeChipColor, MenuRow } from "./menus/part-1";
import { useMenus } from "./menus/useMenus";

export function Menus() {
  const {
    t,
    canCreate,
    canUpdate,
    canDelete,
    menus,
    setMenus,
    parentTree,
    setParentTree,
    loading,
    setLoading,
    reload,
    expandNode,
    keyword,
    setKeyword,
    appliedKeyword,
    setAppliedKeyword,
    viewMode,
    setViewMode,
    flatMenus,
    flatTotal,
    flatPage,
    setFlatPage,
    flatPageSize,
    setFlatPageSize,
    flatLoading,
    reloadFlat,
    refreshAll,
    crud,
    openCreate,
    parentOptions,
    headers,
  } = useMenus();

  return (
    <PageStack>
      <PageHeader
        title={t("nav.menus")}
        subtitle={t("system.menus.configureNavigationTreeAndButtonPermissions")}
        actions={
          canCreate ? (
            <Button variant="contained" onClick={() => openCreate()}>
              {t("system.menus.createMenu")}
            </Button>
          ) : undefined
        }
      />

      <QueryForm
        onSearch={() => {
          setAppliedKeyword(keyword.trim());
          setViewMode("flat");
          setFlatPage(1);
        }}
      >
        <TextField
          label={t("common.keyword")}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <Button
          variant="outlined"
          onClick={() => {
            setKeyword("");
            setAppliedKeyword("");
            setViewMode("tree");
          }}
        >
          {t("system.common.treeView")}
        </Button>
      </QueryForm>

      {viewMode === "flat" ? (
        <>
          <DataTable
            columns={[
              {
                key: "name",
                title: t("common.name"),
                render: (r: MenuDto) => <Typography variant="body2">{t(r.name)}</Typography>,
              },
              {
                key: "type",
                title: t("common.type"),
                render: (r: MenuDto) => (
                  <Chip label={r.type} color={typeChipColor[r.type] ?? "default"} />
                ),
              },
              { key: "path", title: t("common.path") },
              { key: "permission", title: t("system.menus.permissionCode") },
              { key: "sortOrder", title: t("common.sort") },
            ]}
            data={flatMenus}
            loading={flatLoading}
            rowKey={(r) => r.id}
          />
          <Pagination
            page={flatPage}
            pageSize={flatPageSize}
            total={flatTotal}
            onPage={setFlatPage}
            onPageSize={setFlatPageSize}
          />
        </>
      ) : (
        <ContentCard padding={false}>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  {headers.map((h) => (
                    <TableCell key={h}>
                      <Typography
                        variant="caption"
                        color="text.secondary"
                        sx={{ fontWeight: 700, textTransform: "uppercase" }}
                      >
                        {h}
                      </Typography>
                    </TableCell>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center" sx={{ py: 8 }}>
                      <Typography color="text.secondary">{t("common.loading")}</Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  menus.map((item) => (
                    <MenuRow
                      key={item.id}
                      item={item}
                      onEdit={canUpdate ? crud.openEdit : () => {}}
                      onAddChild={canCreate ? (m) => openCreate(m.id) : () => {}}
                      onDelete={canDelete ? crud.confirmDelete : () => {}}
                      onExpand={expandNode}
                    />
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </ContentCard>
      )}

      <Dialog
        open={crud.open}
        onClose={crud.closeDialog}
        title={t(crud.edit ? "system.menus.editMenu" : "system.menus.createMenu2")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <Select
            label={t("common.parent")}
            value={crud.form.parentId}
            onChange={(e) => crud.setForm((f) => ({ ...f, parentId: e.target.value }))}
            options={parentOptions}
          />
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                label={t("common.nameRequired")}
                placeholder={t("nav.users")}
                value={crud.form.name}
                onChange={(e) => crud.setForm((f) => ({ ...f, name: e.target.value }))}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <Select
                label={t("common.typeRequired")}
                value={crud.form.type}
                onChange={(e) => crud.setForm((f) => ({ ...f, type: e.target.value as MenuType }))}
                options={[
                  { value: "directory", label: t("system.menus.directory") },
                  { value: "menu", label: t("system.menus.menu") },
                  { value: "button", label: t("system.menus.button") },
                ]}
              />
            </Grid>
          </Grid>
          <TextField
            label={t("common.path")}
            placeholder={t("system.menus.placeholderMenuPath")}
            value={crud.form.path}
            onChange={(e) => crud.setForm((f) => ({ ...f, path: e.target.value }))}
          />
          <TextField
            label={t("system.menus.component")}
            placeholder={t("system.menus.placeholderMenuComponent")}
            value={crud.form.component}
            onChange={(e) => crud.setForm((f) => ({ ...f, component: e.target.value }))}
          />
          <TextField
            label={t("system.menus.permissionCode")}
            placeholder={t("system.menus.placeholderMenuPermission")}
            value={crud.form.permission}
            onChange={(e) => crud.setForm((f) => ({ ...f, permission: e.target.value }))}
          />
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                label={t("common.sortOrder")}
                type="number"
                placeholder="1"
                value={String(crud.form.sortOrder)}
                onChange={(e) => crud.setForm((f) => ({ ...f, sortOrder: Number(e.target.value) }))}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <Select
                label={t("common.visible")}
                value={crud.form.visible ? "true" : "false"}
                onChange={(e) =>
                  crud.setForm((f) => ({ ...f, visible: e.target.value === "true" }))
                }
                options={[
                  { value: "true", label: t("common.yes") },
                  { value: "false", label: t("common.no") },
                ]}
              />
            </Grid>
          </Grid>
          <Select
            label={t("common.enabled")}
            value={crud.form.enabled ? "true" : "false"}
            onChange={(e) => crud.setForm((f) => ({ ...f, enabled: e.target.value === "true" }))}
            options={[
              { value: "true", label: t("common.enabled") },
              { value: "false", label: t("common.disabled") },
            ]}
          />
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={crud.closeDialog}>
            {t("common.cancel")}
          </Button>
          <Button variant="contained" disabled={crud.busy} onClick={() => void crud.handleSave()}>
            {crud.busy ? t("common.saving") : t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={crud.deleting !== null}
        onClose={crud.closeDelete}
        onConfirm={() => void crud.runDelete()}
        title={t("system.menus.deleteMenu")}
        description={t("system.menus.delete.withAsterisk").replace(
          "{name}",
          crud.deleting ? t(crud.deleting.name) : "",
        )}
      />
    </PageStack>
  );
}

export default Menus;
