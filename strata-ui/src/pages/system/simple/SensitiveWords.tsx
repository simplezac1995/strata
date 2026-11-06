// SPDX-License-Identifier: Apache-2.0
import { Box, DialogActions, Stack } from "@mui/material";
import {
  Button,
  TextField,
  Dialog,
  ConfirmDialog,
  ExportCsvButton,
  PageHeader,
  DataTable,
  Pagination,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { sensitiveWordApi } from "@/shared/api/system";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import { exportCsvFromColumns, type CsvColumn } from "@/shared/utils/csv";
import type { SensitiveWordDto } from "@/shared/types";

export function SensitiveWords() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("system:sensitiveword:create"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:sensitiveword:delete"));

  const {
    rows: words,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    error,
    reload,
  } = usePagedQuery<SensitiveWordDto>(["api", "system", "sensitive-words"], (pageIndex, size) =>
    sensitiveWordApi.page(undefined, pageIndex, size),
  );

  const {
    busy,
    open,
    deleting,
    form,
    setForm,
    openCreate,
    closeDialog,
    handleSave,
    confirmDelete,
    closeDelete,
    runDelete,
  } = useResourceCrud<SensitiveWordDto, string, string, string>({
    create: (word) => sensitiveWordApi.create(word),
    remove: (id) => sensitiveWordApi.remove(id),
    reload,
    initForm: () => "",
    toEditForm: (w) => w.word,
  });

  const csvColumns: CsvColumn<SensitiveWordDto>[] = [
    { title: t("system.sensitiveWords.word"), cell: (w) => w.word },
  ];

  const cols = [
    { key: "id", title: t("common.id"), width: 64 },
    {
      key: "word",
      title: t("system.sensitiveWords.word"),
      render: (r: SensitiveWordDto) => (
        <Box component="code" sx={{ fontSize: 12, fontFamily: "monospace", color: "error.main" }}>
          {r.word}
        </Box>
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: SensitiveWordDto) =>
        canDelete ? (
          <Button variant="text" size="sm" color="danger" onClick={() => confirmDelete(r)}>
            {t("common.delete")}
          </Button>
        ) : null,
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("nav.sensitiveWords")}
        subtitle={t("system.sensitiveWords.subtitle")}
        actions={
          <Stack direction="row" spacing={1}>
            <ExportCsvButton
              onClick={() => exportCsvFromColumns("sensitive-words.csv", csvColumns, words)}
            />
            {canCreate ? (
              <Button variant="contained" onClick={openCreate}>
                {t("system.sensitiveWords.add")}
              </Button>
            ) : null}
          </Stack>
        }
      />
      <DataTable
        columns={cols}
        data={words}
        loading={loading}
        rowKey={(r) => r.id}
        tableId="sensitive-words"
        error={error}
        onRetry={reload}
        emptyAction={
          canCreate ? (
            <Button variant="contained" onClick={openCreate}>
              {t("system.sensitiveWords.add")}
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

      <Dialog open={open} onClose={closeDialog} title={t("system.sensitiveWords.add")}>
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("system.sensitiveWords.word")}
            value={form}
            onChange={(e) => setForm(e.target.value)}
            placeholder={t("system.sensitiveWords.placeholder")}
          />
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={closeDialog}>
            {t("common.cancel")}
          </Button>
          <Button
            variant="contained"
            disabled={busy || !form.trim()}
            onClick={() => void handleSave()}
          >
            {busy ? t("common.saving") : t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={deleting !== null}
        onClose={closeDelete}
        onConfirm={() => void runDelete()}
        title={t("common.delete")}
        description={t("common.deleteNamed").replace("{name}", deleting?.word ?? "")}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default SensitiveWords;
