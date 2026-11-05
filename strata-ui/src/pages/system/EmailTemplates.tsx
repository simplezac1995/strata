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
  RichTextEditor,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import {
  emailTemplateApi,
  type EmailTemplateCreatePayload,
  type EmailTemplateUpdatePayload,
} from "@/shared/api/system";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import type { EmailTemplateDto } from "@/shared/types";

export function EmailTemplates() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("system:emailtemplate:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("system:emailtemplate:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:emailtemplate:delete"));

  const {
    rows: templates,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    error,
    reload,
  } = usePagedQuery<EmailTemplateDto>(["api", "system", "email-templates"], emailTemplateApi.page);

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
    EmailTemplateDto,
    EmailTemplateCreatePayload,
    EmailTemplateUpdatePayload,
    { code: string; name: string; subject: string; body: string; enabled: boolean }
  >({
    create: (payload) => emailTemplateApi.create(payload),
    update: (id, payload) => emailTemplateApi.update(id, payload),
    remove: (id) => emailTemplateApi.remove(id),
    reload,
    initForm: () => ({ code: "", name: "", subject: "", body: "", enabled: true }),
    toEditForm: (r) => ({
      code: r.code,
      name: r.name,
      subject: r.subject,
      body: r.body ?? "",
      enabled: r.enabled,
    }),
    toCreatePayload: (f) => ({
      code: f.code,
      name: f.name,
      subject: f.subject,
      body: f.body,
      enabled: f.enabled,
    }),
    toUpdatePayload: (f) => ({
      name: f.name,
      subject: f.subject,
      body: f.body,
      enabled: f.enabled,
    }),
  });

  const cols = [
    {
      key: "code",
      title: t("system.emailTemplates.code"),
      render: (r: EmailTemplateDto) => (
        <Box component="code" sx={{ fontSize: 12, fontFamily: "monospace", color: "primary.main" }}>
          {r.code}
        </Box>
      ),
    },
    { key: "name", title: t("system.emailTemplates.name") },
    { key: "subject", title: t("system.emailTemplates.subject") },
    {
      key: "enabled",
      title: t("common.status"),
      render: (r: EmailTemplateDto) => <StatusChip enabled={r.enabled} />,
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: EmailTemplateDto) => (
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
        title={t("nav.emailTemplates")}
        subtitle={t("system.emailTemplates.subtitle")}
        actions={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              {t("system.emailTemplates.create")}
            </Button>
          ) : undefined
        }
      />
      <DataTable
        columns={cols}
        data={templates}
        loading={loading}
        rowKey={(r) => r.id}
        tableId="email-templates"
        error={error}
        onRetry={reload}
        emptyAction={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              {t("system.emailTemplates.create")}
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
        title={edit ? t("system.emailTemplates.edit") : t("system.emailTemplates.create")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("system.emailTemplates.code")}
            value={form.code}
            onChange={(e) => setForm((f) => ({ ...f, code: e.target.value }))}
            disabled={!!edit}
          />
          <TextField
            label={t("system.emailTemplates.name")}
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
          />
          <TextField
            label={t("system.emailTemplates.subject")}
            value={form.subject}
            onChange={(e) => setForm((f) => ({ ...f, subject: e.target.value }))}
          />
          <Box>
            <Typography variant="body2" sx={{ mb: 0.5, color: "text.secondary" }}>
              {t("system.emailTemplates.body")}
            </Typography>
            <RichTextEditor
              value={form.body}
              onChange={(html) => setForm((f) => ({ ...f, body: html }))}
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
        description={t("common.deleteNamed").replace("{name}", deleting?.name ?? "")}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default EmailTemplates;
