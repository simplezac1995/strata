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
  StatusChip,
  Pagination,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import {
  clientApi,
  type OauthClientCreatePayload,
  type OauthClientUpdatePayload,
} from "@/shared/api/system";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import type { OauthClientDto } from "@/shared/types";

export function Clients() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("system:client:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("system:client:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:client:delete"));

  const {
    rows: clients,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    error,
    reload,
  } = usePagedQuery<OauthClientDto>(["api", "system", "clients"], clientApi.page);

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
    OauthClientDto,
    OauthClientCreatePayload,
    OauthClientUpdatePayload,
    { clientId: string; name: string; clientSecret: string; scopes: string; enabled: boolean }
  >({
    create: (payload) => clientApi.create(payload),
    update: (id, payload) => clientApi.update(id, payload),
    remove: (id) => clientApi.remove(id),
    reload,
    initForm: () => ({ clientId: "", name: "", clientSecret: "", scopes: "", enabled: true }),
    toEditForm: (r) => ({
      clientId: r.clientId,
      name: r.name,
      clientSecret: "",
      scopes: r.scopes ?? "",
      enabled: r.enabled,
    }),
  });

  const cols = [
    {
      key: "clientId",
      title: t("system.clients.clientId"),
      render: (r: OauthClientDto) => (
        <Box component="code" sx={{ fontSize: 12, fontFamily: "monospace", color: "primary.main" }}>
          {r.clientId}
        </Box>
      ),
    },
    { key: "name", title: t("system.clients.name") },
    {
      key: "scopes",
      title: t("system.clients.scopes"),
      render: (r: OauthClientDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.secondary">
          {r.scopes}
        </Typography>
      ),
    },
    {
      key: "enabled",
      title: t("common.status"),
      render: (r: OauthClientDto) => <StatusChip enabled={r.enabled} />,
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: OauthClientDto) => (
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
        title={t("nav.clients")}
        subtitle={t("system.clients.subtitle")}
        actions={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              {t("system.clients.create")}
            </Button>
          ) : undefined
        }
      />
      <DataTable
        columns={cols}
        data={clients}
        loading={loading}
        rowKey={(r) => r.id}
        tableId="clients"
        error={error}
        onRetry={reload}
        emptyAction={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              {t("system.clients.create")}
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
        title={edit ? t("system.clients.edit") : t("system.clients.create")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("system.clients.clientId")}
            value={form.clientId}
            onChange={(e) => setForm((f) => ({ ...f, clientId: e.target.value }))}
          />
          <TextField
            label={t("system.clients.name")}
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
          />
          <TextField
            label={t("system.clients.clientSecret")}
            type="password"
            value={form.clientSecret}
            onChange={(e) => setForm((f) => ({ ...f, clientSecret: e.target.value }))}
            placeholder={edit ? t("system.clients.leaveBlankToKeep") : ""}
          />
          <TextField
            label={t("system.clients.scopes")}
            value={form.scopes}
            onChange={(e) => setForm((f) => ({ ...f, scopes: e.target.value }))}
            placeholder={"read write"}
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

export default Clients;
