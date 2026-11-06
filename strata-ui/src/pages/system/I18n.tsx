// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, DialogActions, Stack, Typography } from "@mui/material";
import {
  Button,
  TextField,
  Dialog,
  PageHeader,
  QueryForm,
  DataTable,
  Pagination,
  ConfirmDialog,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { i18nEntryApi } from "@/shared/api/system";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { useAuthStore } from "@/shared/auth/store";
import type { I18nEntryDto } from "@/shared/types";

export function I18n() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("system:i18n:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("system:i18n:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:i18n:delete"));

  const [keyword, setKeyword] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");

  const { rows, total, page, setPage, pageSize, setPageSize, loading, reload } =
    usePagedQuery<I18nEntryDto>(["api", "system", "i18n", { appliedKeyword }], (pageIndex, size) =>
      i18nEntryApi.page(appliedKeyword, pageIndex, size),
    );

  const createMutation = useMutationWithToast(
    (payload: { messageKey: string; zh: string; en: string }) => i18nEntryApi.create(payload),
    { success: "toast.saved", failure: "toast.saveFailed" },
    { onSuccess: () => reload() },
  );

  const updateMutation = useMutationWithToast(
    ({ id, payload }: { id: string; payload: { zh: string; en: string } }) =>
      i18nEntryApi.update(id, payload),
    { success: "toast.saved", failure: "toast.saveFailed" },
    { onSuccess: () => reload() },
  );

  const deleteMutation = useMutationWithToast(
    (id: string) => i18nEntryApi.remove(id),
    { success: "toast.deleted", failure: "toast.deleteFailed" },
    { onSuccess: () => reload() },
  );

  const busy = createMutation.isPending || updateMutation.isPending || deleteMutation.isPending;

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editEntry, setEditEntry] = useState<I18nEntryDto | null>(null);
  const [deleteEntry, setDeleteEntry] = useState<I18nEntryDto | null>(null);
  const [form, setForm] = useState({ messageKey: "", zh: "", en: "" });

  function openCreate() {
    setEditEntry(null);
    setForm({ messageKey: "", zh: "", en: "" });
    setDialogOpen(true);
  }

  function openEdit(d: I18nEntryDto) {
    setEditEntry(d);
    setForm({ messageKey: d.messageKey, zh: d.zh ?? "", en: d.en ?? "" });
    setDialogOpen(true);
  }

  async function handleSave() {
    try {
      if (editEntry) {
        await updateMutation.mutateAsync({
          id: editEntry.id,
          payload: { zh: form.zh, en: form.en },
        });
      } else {
        await createMutation.mutateAsync({
          messageKey: form.messageKey,
          zh: form.zh,
          en: form.en,
        });
      }
      setDialogOpen(false);
    } catch {
      // toast already shown
    }
  }

  const columns = [
    {
      key: "key",
      title: t("common.key"),
      render: (d: I18nEntryDto) => (
        <Box component="code" sx={{ fontSize: 12, fontFamily: "monospace", color: "primary.main" }}>
          {d.messageKey}
        </Box>
      ),
    },
    {
      key: "zh",
      title: t("system.i18n.langChinese"),
      render: (d: I18nEntryDto) => <Typography variant="body2">{d.zh}</Typography>,
    },
    {
      key: "en",
      title: t("system.i18n.langEnglish"),
      render: (d: I18nEntryDto) => <Typography variant="body2">{d.en}</Typography>,
    },
    {
      key: "updatedBy",
      title: t("system.i18n.updatedBy"),
      render: (d: I18nEntryDto) => (
        <Typography variant="caption" color="text.secondary">
          {d.updatedBy ?? "—"}
        </Typography>
      ),
    },
    {
      key: "updatedAt",
      title: t("system.i18n.updatedAt"),
      render: (d: I18nEntryDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.secondary">
          {d.updatedAt}
        </Typography>
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (d: I18nEntryDto) => (
        <Stack direction="row" spacing={0.5}>
          <Button variant="text" size="sm" disabled={!canUpdate} onClick={() => openEdit(d)}>
            {t("common.edit")}
          </Button>
          <Button
            variant="text"
            size="sm"
            color="danger"
            disabled={!canDelete}
            onClick={() => setDeleteEntry(d)}
          >
            {t("common.delete")}
          </Button>
        </Stack>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("system.common.internationalization")}
        subtitle={t("system.i18n.manageTranslationKeysForAllSupported")}
        actions={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              {t("system.i18n.plusCreate")}
            </Button>
          ) : undefined
        }
      />

      <QueryForm
        onSearch={() => {
          setPage(1);
          setAppliedKeyword(keyword);
        }}
        onReset={() => {
          setKeyword("");
          setAppliedKeyword("");
          setPage(1);
        }}
      >
        <TextField
          label={t("common.key")}
          placeholder={t("system.i18n.placeholderI18nKey")}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
      </QueryForm>

      <DataTable columns={columns} data={rows} rowKey={(d) => d.id} loading={loading} />
      <Pagination
        total={total}
        page={page}
        pageSize={pageSize}
        onPage={setPage}
        onPageSize={setPageSize}
      />

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={editEntry ? t("system.i18n.editTranslation") : t("system.i18n.createTranslation")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("common.key")}
            placeholder={t("system.i18n.placeholderI18nKey")}
            value={form.messageKey}
            disabled={!!editEntry}
            onChange={(e) => setForm((f) => ({ ...f, messageKey: e.target.value }))}
          />
          <TextField
            label={t("system.i18n.langChinese")}
            placeholder={t("system.i18n.placeholderI18nZh")}
            value={form.zh}
            onChange={(e) => setForm((f) => ({ ...f, zh: e.target.value }))}
          />
          <TextField
            label={t("system.i18n.langEnglish")}
            placeholder={t("system.i18n.placeholderI18nEn")}
            value={form.en}
            onChange={(e) => setForm((f) => ({ ...f, en: e.target.value }))}
          />
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={() => setDialogOpen(false)}>
            {t("common.cancel")}
          </Button>
          <Button variant="contained" disabled={busy} onClick={() => void handleSave()}>
            {busy ? t("common.saving") : t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={!!deleteEntry}
        onClose={() => setDeleteEntry(null)}
        title={t("common.delete")}
        description={t("system.i18n.deleteTranslationKey")}
        onConfirm={async () => {
          if (!deleteEntry) return;
          try {
            await deleteMutation.mutateAsync(deleteEntry.id);
            setDeleteEntry(null);
          } catch {
            // toast already shown
          }
        }}
      />
    </PageStack>
  );
}

export default I18n;
