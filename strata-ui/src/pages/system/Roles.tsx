// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, Checkbox, DialogActions, IconButton, Stack, Typography } from "@mui/material";
import {
  Button,
  TextField,
  Select,
  Dialog,
  ConfirmDialog,
  ExportCsvButton,
  PageHeader,
  DataTable,
  Pagination,
  StatusChip,
  Chip,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import {
  menuApi,
  roleApi,
  type RoleCreatePayload,
  type RoleUpdatePayload,
} from "@/shared/api/system";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import { exportCsvFromColumns, type CsvColumn } from "@/shared/utils/csv";
import type { MenuDto, RoleDto } from "@/shared/types";

const typeChipColor: Record<string, string> = {
  directory: "warning",
  menu: "info",
  button: "secondary",
};

function MenuTreeNode({
  node,
  checked,
  onToggle,
  depth = 0,
}: {
  node: MenuDto;
  checked: Set<string>;
  onToggle: (id: string) => void;
  depth?: number;
}) {
  const { t } = useI18n();
  const [open, setOpen] = useState(true);
  return (
    <Box>
      <Stack
        direction="row"
        spacing={1}
        sx={{
          alignItems: "center",
          py: 0.75,
          px: 1,
          pl: `${depth * 20 + 8}px`,
          borderRadius: 2,
          "&:hover": { bgcolor: "action.hover" },
        }}
      >
        <Checkbox size="small" checked={checked.has(node.id)} onChange={() => onToggle(node.id)} />
        {node.children.length > 0 ? (
          <IconButton
            size="small"
            aria-label={open ? t("common.collapse") : t("common.expand")}
            onClick={() => setOpen((o) => !o)}
            sx={{ width: 16, height: 16, fontSize: 12 }}
          >
            {open ? "\u25BE" : "\u25B8"}
          </IconButton>
        ) : (
          <Box sx={{ width: 16, flexShrink: 0 }} />
        )}
        <Typography variant="body2" sx={{ flex: 1 }}>
          {t(node.name)}
        </Typography>
        {node.permission ? (
          <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.disabled">
            {node.permission}
          </Typography>
        ) : null}
        <Chip label={node.type} color={typeChipColor[node.type] ?? "default"} />
      </Stack>
      {open
        ? node.children.map((child) => (
            <MenuTreeNode
              key={child.id}
              node={child}
              checked={checked}
              onToggle={onToggle}
              depth={depth + 1}
            />
          ))
        : null}
    </Box>
  );
}

export function Roles() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("system:role:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("system:role:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:role:delete"));

  const {
    rows: roles,
    total: totalRoles,
    page: rolePage,
    setPage: setRolePage,
    pageSize: rolePageSize,
    setPageSize: setRolePageSize,
    loading,
    reload,
    sort,
    setSort,
    error,
  } = usePagedQuery<RoleDto>(["api", "system", "roles"], (pageIndex, pageSize, sortState) =>
    roleApi.page("", pageIndex, pageSize, sortState?.field, sortState?.dir),
  );
  const { data: menus } = useListQuery<MenuDto>(["api", "system", "menus", "tree"], menuApi.list, {
    staleTime: 5 * 60 * 1000,
  });

  const [checked, setChecked] = useState<Set<string>>(new Set());

  const crud = useResourceCrud<
    RoleDto,
    RoleCreatePayload,
    RoleUpdatePayload,
    { code: string; name: string; enabled: boolean }
  >({
    create: (payload) => roleApi.create(payload),
    update: (id, payload) => roleApi.update(id, payload),
    remove: (id) => roleApi.remove(id),
    reload,
    initForm: () => ({ code: "", name: "", enabled: true }),
    toEditForm: (r) => ({ code: r.code, name: r.name, enabled: r.enabled }),
    toCreatePayload: (f) => ({
      code: f.code,
      name: f.name,
      enabled: f.enabled,
      menuIds: [...checked],
    }),
    toUpdatePayload: (f) => ({
      name: f.name,
      enabled: f.enabled,
      menuIds: [...checked],
    }),
  });

  function toggleMenu(id: string) {
    setChecked((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  function openCreateWithReset() {
    crud.openCreate();
    setChecked(new Set());
  }

  function openEditWithReset(r: RoleDto) {
    crud.openEdit(r);
    setChecked(new Set(r.menuIds));
  }

  const csvColumns: CsvColumn<RoleDto>[] = [
    { title: t("common.code"), cell: (r) => r.code },
    { title: t("common.name"), cell: (r) => r.name },
    {
      title: t("common.status"),
      cell: (r) => (r.enabled ? t("common.enabled") : t("common.disabled")),
    },
    { title: t("system.roles.menuCount"), cell: (r) => r.menuIds.length },
  ];

  const columns = [
    {
      key: "code",
      title: t("common.code"),
      sortable: true,
      render: (r: RoleDto) => (
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
          {r.code}
        </Box>
      ),
    },
    { key: "name", title: t("common.name"), sortable: true },
    {
      key: "enabled",
      title: t("common.status"),
      sortable: true,
      render: (r: RoleDto) => <StatusChip enabled={r.enabled} />,
    },
    {
      key: "menuCount",
      title: t("system.roles.menuCount"),
      render: (r: RoleDto) => (
        <Typography variant="body2" color="text.secondary">
          {r.menuIds.length}
        </Typography>
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: RoleDto) => (
        <Stack direction="row" spacing={0.5}>
          {canUpdate && (
            <Button variant="text" size="sm" onClick={() => openEditWithReset(r)}>
              {t("common.edit")}
            </Button>
          )}
          {canDelete && (
            <Button variant="text" size="sm" color="danger" onClick={() => crud.confirmDelete(r)}>
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
        title={t("nav.roles")}
        subtitle={t("system.roles.subtitle")}
        actions={
          <Stack direction="row" spacing={1}>
            <ExportCsvButton onClick={() => exportCsvFromColumns("roles.csv", csvColumns, roles)} />
            {canCreate ? (
              <Button variant="contained" onClick={openCreateWithReset}>
                {t("system.roles.createButton")}
              </Button>
            ) : null}
          </Stack>
        }
      />

      <DataTable
        tableId="roles"
        columns={columns}
        data={roles}
        loading={loading}
        rowKey={(r) => r.id}
        sort={sort}
        onSortChange={setSort}
        error={error}
        onRetry={reload}
        emptyAction={
          canCreate ? (
            <Button variant="contained" onClick={openCreateWithReset}>
              {t("system.roles.createButton")}
            </Button>
          ) : undefined
        }
      />
      <Pagination
        total={totalRoles}
        page={rolePage}
        pageSize={rolePageSize}
        onPage={setRolePage}
        onPageSize={setRolePageSize}
      />

      <Dialog
        open={crud.open}
        onClose={crud.closeDialog}
        title={t(crud.edit ? "system.roles.editTitle" : "system.roles.createTitle")}
        maxWidth="md"
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
            <TextField
              label={t("common.code")}
              value={crud.form.code}
              disabled={!!crud.edit}
              onChange={(e) => crud.setForm((f) => ({ ...f, code: e.target.value }))}
              sx={{ flex: 1 }}
            />
            <TextField
              label={t("common.name")}
              value={crud.form.name}
              onChange={(e) => crud.setForm((f) => ({ ...f, name: e.target.value }))}
              sx={{ flex: 1 }}
            />
          </Stack>
          <Select
            label={t("common.status")}
            value={crud.form.enabled ? "true" : "false"}
            onChange={(e) => crud.setForm((f) => ({ ...f, enabled: e.target.value === "true" }))}
            options={[
              { value: "true", label: t("common.enabled") },
              { value: "false", label: t("common.disabled") },
            ]}
          />
          <Box>
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ fontWeight: 600, display: "block", mb: 1 }}
            >
              {t("system.roles.menuPermissions")}
            </Typography>
            <Box
              sx={{
                border: 1,
                borderColor: "divider",
                borderRadius: 3,
                p: 1,
                maxHeight: 320,
                overflowY: "auto",
              }}
            >
              {menus.map((node) => (
                <MenuTreeNode key={node.id} node={node} checked={checked} onToggle={toggleMenu} />
              ))}
            </Box>
          </Box>
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
        title={t("system.roles.deleteTitle")}
        description={t("system.roles.deleteDesc").replace("{name}", crud.deleting?.name ?? "")}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default Roles;
