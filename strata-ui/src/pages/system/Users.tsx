// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, Checkbox, DialogActions, FormControlLabel, Stack, Typography } from "@mui/material";
import {
  Button,
  TextField,
  Select,
  Dialog,
  ConfirmDialog,
  ExportCsvButton,
  PageHeader,
  QueryForm,
  DataTable,
  StatusChip,
  Chip,
  Pagination,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import {
  roleApi,
  userApi,
  type UserCreatePayload,
  type UserUpdatePayload,
} from "@/shared/api/system";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { usePagedQuery, type SortState } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { IfPerm } from "@/shared/pageKit/IfPerm";
import client from "@/shared/api/client";
import { exportCsvFromColumns, type CsvColumn } from "@/shared/utils/csv";
import { toast } from "@/shared/pageKit/Toast";
import type { RoleDto, UserDto } from "@/shared/types";

export function Users() {
  const { t } = useI18n();
  const canUpdate = useAuthStore((s) => s.hasAuthority("system:user:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:user:delete"));

  const [filterUser, setFilterUser] = useState("");
  const {
    rows: users,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    reload,
    onSearch,
    sort,
    setSort,
    error,
  } = usePagedQuery<UserDto>(
    ["api", "system", "users", { filterUser }],
    (pageIndex, size, sortState) =>
      userApi.page(filterUser, pageIndex, size, sortState?.field, sortState?.dir),
  );
  const { data: roles } = useListQuery<RoleDto>(["api", "system", "roles", "list"], roleApi.list, {
    staleTime: 5 * 60 * 1000,
  });

  const batchDeleteMutation = useMutationWithToast(
    (ids: string[]) => userApi.batchDelete(ids),
    { success: "toast.deleted", failure: "toast.deleteFailed" },
    { onSuccess: () => reload() },
  );

  const [selectedKeys, setSelectedKeys] = useState<Set<string>>(new Set());
  const [batchConfirmOpen, setBatchConfirmOpen] = useState(false);

  const crud = useResourceCrud<
    UserDto,
    UserCreatePayload,
    UserUpdatePayload,
    { username: string; password: string; nickname: string; enabled: boolean; roleIds: string[] }
  >({
    create: (payload) => userApi.create(payload),
    update: (id, payload) => userApi.update(id, payload),
    remove: (id) => userApi.remove(id),
    reload,
    initForm: () => ({
      username: "",
      password: "",
      nickname: "",
      enabled: true,
      roleIds: [],
    }),
    toEditForm: (u) => ({
      username: u.username,
      password: "",
      nickname: u.nickname ?? "",
      enabled: u.enabled,
      roleIds: u.roleIds,
    }),
    toCreatePayload: (f) => ({
      username: f.username,
      password: f.password,
      nickname: f.nickname || null,
      enabled: f.enabled,
      roleIds: f.roleIds,
    }),
    toUpdatePayload: (f) => ({
      nickname: f.nickname || null,
      enabled: f.enabled,
      password: f.password || null,
      roleIds: f.roleIds,
    }),
  });

  const busy = crud.busy || batchDeleteMutation.isPending;

  async function handleBatchDelete() {
    if (selectedKeys.size === 0) return;
    try {
      await batchDeleteMutation.mutateAsync([...selectedKeys]);
      setSelectedKeys(new Set());
    } catch {
      // toast already shown
    }
  }

  async function handleExport() {
    try {
      const res = await client.get(userApi.exportUrl(filterUser), { responseType: "blob" });
      const blob = new Blob([res.data], {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "users.xlsx";
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      toast.error("toast.loadFailed");
    }
  }

  const csvColumns: CsvColumn<UserDto>[] = [
    { title: t("common.username"), cell: (u) => u.username },
    { title: t("common.nickname"), cell: (u) => u.nickname ?? "" },
    {
      title: t("common.status"),
      cell: (u) => (u.enabled ? t("common.enabled") : t("common.disabled")),
    },
    {
      title: t("nav.roles"),
      cell: (u) => u.roleIds.map((id) => roles.find((r) => r.id === id)?.name ?? id).join("; "),
    },
  ];

  const columns = [
    { key: "username", title: t("common.username"), sortable: true },
    { key: "nickname", title: t("common.nickname"), sortable: true },
    {
      key: "enabled",
      title: t("common.status"),
      sortable: true,
      render: (u: UserDto) => <StatusChip enabled={u.enabled} />,
    },
    {
      key: "roles",
      title: t("nav.roles"),
      render: (u: UserDto) => (
        <Stack direction="row" spacing={0.5} sx={{ flexWrap: "wrap" }}>
          {u.roleIds.map((id) => (
            <Chip key={id} label={roles.find((r) => r.id === id)?.name ?? id} color="primary" />
          ))}
        </Stack>
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (u: UserDto) => (
        <Stack direction="row" spacing={0.5}>
          {canUpdate && (
            <Button variant="text" size="sm" onClick={() => crud.openEdit(u)}>
              {t("common.edit")}
            </Button>
          )}
          {canDelete && (
            <Button variant="text" size="sm" color="danger" onClick={() => crud.confirmDelete(u)}>
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
        title={t("nav.users")}
        subtitle={t("system.users.subtitle")}
        actions={
          <Stack direction="row" spacing={1}>
            <ExportCsvButton onClick={() => exportCsvFromColumns("users.csv", csvColumns, users)} />
            <Button variant="outlined" onClick={() => void handleExport()}>
              {t("common.exportExcel")}
            </Button>
            <IfPerm anyOf={["system:user:create"]}>
              <Button variant="contained" onClick={crud.openCreate} data-testid="users-create">
                {t("system.users.createButton")}
              </Button>
            </IfPerm>
          </Stack>
        }
      />

      <QueryForm
        onSearch={onSearch}
        onReset={() => {
          setFilterUser("");
          setPage(1);
        }}
      >
        <TextField
          label={t("system.users.filterLabel")}
          placeholder={t("common.searchPlaceholder")}
          value={filterUser}
          onChange={(e) => setFilterUser(e.target.value)}
        />
        <Select
          label={t("common.sortBy")}
          value={sort?.field ?? "id"}
          onChange={(e) =>
            setSort({
              field: e.target.value,
              dir: sort?.dir ?? "asc",
            } satisfies SortState)
          }
          options={[
            { value: "id", label: t("common.id") },
            { value: "username", label: t("common.username") },
            { value: "nickname", label: t("common.nickname") },
          ]}
        />
        <Select
          label={t("common.sortDirection")}
          value={sort?.dir ?? "asc"}
          onChange={(e) =>
            setSort({
              field: sort?.field ?? "id",
              dir: e.target.value as SortState["dir"],
            })
          }
          options={[
            { value: "asc", label: t("common.sortAsc") },
            { value: "desc", label: t("common.sortDesc") },
          ]}
        />
      </QueryForm>

      <DataTable
        tableId="users"
        columns={columns}
        data={users}
        loading={loading}
        rowKey={(u) => u.id}
        selectable={canDelete}
        selectedKeys={selectedKeys}
        onSelectedKeysChange={setSelectedKeys}
        sort={sort}
        onSortChange={setSort}
        error={error}
        onRetry={reload}
        emptyAction={
          <IfPerm anyOf={["system:user:create"]}>
            <Button variant="contained" onClick={crud.openCreate}>
              {t("system.users.createButton")}
            </Button>
          </IfPerm>
        }
        batchActions={
          <IfPerm anyOf={["system:user:delete"]}>
            <Button
              variant="outlined"
              color="danger"
              size="sm"
              disabled={busy}
              data-testid="users-batch-delete"
              onClick={() => setBatchConfirmOpen(true)}
            >
              {t("common.batchDelete")}
            </Button>
          </IfPerm>
        }
      />
      <Pagination
        total={total}
        page={page}
        pageSize={pageSize}
        onPage={setPage}
        onPageSize={setPageSize}
      />

      <Dialog
        open={crud.open}
        onClose={crud.closeDialog}
        title={t(crud.edit ? "system.users.editTitle" : "system.users.createTitle")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("common.usernameRequired")}
            placeholder={t("auth.login.usernamePlaceholder")}
            value={crud.form.username}
            disabled={!!crud.edit}
            onChange={(e) => crud.setForm((f) => ({ ...f, username: e.target.value }))}
            slotProps={{ htmlInput: { "data-testid": "user-form-username" } }}
          />
          <TextField
            label={t(
              crud.edit ? "system.users.field.passwordKeep" : "system.users.field.passwordRequired",
            )}
            type="password"
            placeholder={t("system.users.passwordPlaceholder")}
            value={crud.form.password}
            onChange={(e) => crud.setForm((f) => ({ ...f, password: e.target.value }))}
            slotProps={{ htmlInput: { "data-testid": "user-form-password" } }}
          />
          <TextField
            label={t("common.nickname")}
            placeholder={t("auth.register.namePlaceholder")}
            value={crud.form.nickname}
            onChange={(e) => crud.setForm((f) => ({ ...f, nickname: e.target.value }))}
          />
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
              {t("nav.roles")}
            </Typography>
            <Box
              sx={{
                border: 1,
                borderColor: "divider",
                borderRadius: 3,
                p: 1.5,
                maxHeight: 160,
                overflowY: "auto",
              }}
            >
              <Stack spacing={0.5}>
                {roles.map((r) => (
                  <FormControlLabel
                    key={r.id}
                    control={
                      <Checkbox
                        size="small"
                        checked={crud.form.roleIds.includes(r.id)}
                        onChange={(e) =>
                          crud.setForm((f) => ({
                            ...f,
                            roleIds: e.target.checked
                              ? [...f.roleIds, r.id]
                              : f.roleIds.filter((id) => id !== r.id),
                          }))
                        }
                      />
                    }
                    label={
                      <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                        <Typography variant="body2">{r.name}</Typography>
                        <Typography variant="caption" color="text.disabled">
                          {r.code}
                        </Typography>
                      </Stack>
                    }
                  />
                ))}
              </Stack>
            </Box>
          </Box>
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={crud.closeDialog}>
            {t("common.cancel")}
          </Button>
          <Button
            variant="contained"
            disabled={busy}
            data-testid="user-form-save"
            onClick={() => void crud.handleSave()}
          >
            {busy ? t("common.saving") : t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={crud.deleting !== null}
        onClose={crud.closeDelete}
        onConfirm={() => void crud.runDelete()}
        title={t("system.users.deleteTitle")}
        description={t("system.users.delete.withAsterisk").replace(
          "{name}",
          crud.deleting?.username ?? "",
        )}
        confirmColor="danger"
      />

      <ConfirmDialog
        open={batchConfirmOpen}
        onClose={() => setBatchConfirmOpen(false)}
        confirmDisabled={busy}
        onConfirm={() => {
          setBatchConfirmOpen(false);
          void handleBatchDelete();
        }}
        title={t("common.batchDeleteTitle")}
        description={t("common.batchDeleteConfirm").replace("{n}", String(selectedKeys.size))}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default Users;
