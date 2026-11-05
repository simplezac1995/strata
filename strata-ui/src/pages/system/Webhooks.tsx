// SPDX-License-Identifier: Apache-2.0
import { Box, DialogActions, Stack } from "@mui/material";
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
import { webhookApi, type WebhookPayload } from "@/shared/api/system";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import type { WebhookDto } from "@/shared/types";

export function Webhooks() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("system:webhook:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("system:webhook:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:webhook:delete"));

  const {
    rows: webhooks,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    error,
    reload,
  } = usePagedQuery<WebhookDto>(["api", "system", "webhooks"], webhookApi.page);

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
    WebhookDto,
    WebhookPayload,
    WebhookPayload,
    { name: string; url: string; secret: string; enabled: boolean }
  >({
    create: (payload) => webhookApi.create(payload),
    update: (id, payload) => webhookApi.update(id, payload),
    remove: (id) => webhookApi.remove(id),
    reload,
    initForm: () => ({ name: "", url: "", secret: "", enabled: true }),
    toEditForm: (r) => ({
      name: r.name,
      url: r.url,
      secret: r.secret ?? "",
      enabled: r.enabled,
    }),
    toCreatePayload: (f) => ({
      name: f.name,
      url: f.url,
      secret: f.secret.trim() ? f.secret : null,
      enabled: f.enabled,
    }),
    toUpdatePayload: (f) => ({
      name: f.name,
      url: f.url,
      secret: f.secret.trim() ? f.secret : null,
      enabled: f.enabled,
    }),
  });

  const cols = [
    { key: "name", title: t("system.webhooks.name") },
    {
      key: "url",
      title: t("system.webhooks.url"),
      render: (r: WebhookDto) => (
        <Box
          component="code"
          sx={{ fontSize: 12, fontFamily: "monospace", color: "text.secondary" }}
        >
          {r.url}
        </Box>
      ),
    },
    {
      key: "enabled",
      title: t("common.status"),
      render: (r: WebhookDto) => <StatusChip enabled={r.enabled} />,
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: WebhookDto) => (
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
        title={t("nav.webhooks")}
        subtitle={t("system.webhooks.subtitle")}
        actions={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              {t("system.webhooks.create")}
            </Button>
          ) : undefined
        }
      />
      <DataTable
        columns={cols}
        data={webhooks}
        loading={loading}
        rowKey={(r) => r.id}
        tableId="webhooks"
        error={error}
        onRetry={reload}
        emptyAction={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              {t("system.webhooks.create")}
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
        title={edit ? t("system.webhooks.edit") : t("system.webhooks.create")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("system.webhooks.name")}
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
          />
          <TextField
            label={t("system.webhooks.url")}
            value={form.url}
            onChange={(e) => setForm((f) => ({ ...f, url: e.target.value }))}
            placeholder="https://example.com/webhook"
          />
          <TextField
            label={t("system.webhooks.secret")}
            type="password"
            value={form.secret}
            onChange={(e) => setForm((f) => ({ ...f, secret: e.target.value }))}
            placeholder={edit ? t("system.webhooks.leaveBlankToKeep") : ""}
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

export default Webhooks;
