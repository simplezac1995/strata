import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { {{EL}}Api } from "@/shared/api/{{MODULE}}";
import type { {{E}}Dto, {{E}}Payload, {{E}}PageQuery } from "@/shared/types";
import {
  Button,
  DataTable,
  Dialog,
  DialogActions,
  ConfirmDialog,
  PageHeader,
  Pagination,
  QueryField,
  QueryForm,
  Select,
  StatusChip,
  TextField,
} from "@/components/ui";
import { useState } from "react";

export default function {{E}}List() {
  const { t } = useI18n();
  const { hasPermission } = useAuthStore();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
{{FILTER_STATE_DECLS}}  const [filter, setFilter] = useState<{{E}}PageQuery>({
{{FILTER_QUERY_KEYS_INIT}}  });
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<{{E}}Dto | null>(null);
  const [form, setForm] = useState<{{E}}Payload>({{EMPTY_FORM_INIT}});

  const { data, isLoading } = usePagedQuery({
    queryKey: ["{{MODULE}}-{{RESOURCE}}", page, size, filter],
    queryFn: () => {{EL}}Api.page(page, size, filter),
  });

  const { mutate: create } = useMutationWithToast({
    mutationFn: {{EL}}Api.create,
    successMsg: t("common.createSuccess"),
    invalidateKeys: [["{{MODULE}}-{{RESOURCE}}"]],
  });

  const { mutate: update } = useMutationWithToast({
    mutationFn: (payload: { id: string; data: {{E}}Payload }) =>
      {{EL}}Api.update(payload.id, payload.data),
    successMsg: t("common.updateSuccess"),
    invalidateKeys: [["{{MODULE}}-{{RESOURCE}}"]],
  });

  const { mutate: remove } = useMutationWithToast({
    mutationFn: {{EL}}Api.remove,
    successMsg: t("common.deleteSuccess"),
    invalidateKeys: [["{{MODULE}}-{{RESOURCE}}"]],
  });

  const columns = [
{{COLUMNS}}  ];

  const handleSearch = () => {
    setPage(0);
    setFilter({
{{FILTER_QUERY_KEYS_SEARCH}}    });
  };

  const handleReset = () => {
    setPage(0);
{{FILTER_RESET}}  };

  const openCreate = () => {
    setEditing(null);
    setForm({{EMPTY_FORM_CREATE}});
    setDialogOpen(true);
  };

  const openEdit = (row: {{E}}Dto) => {
    setEditing(row);
    setForm({{EDIT_FORM_MAP}});
    setDialogOpen(true);
  };

  const handleSubmit = () => {
    if (editing) {
      update({ id: editing.id, data: form });
    } else {
      create(form);
    }
    setDialogOpen(false);
  };

  return (
    <PageStack>
      <PageHeader
        title={t("nav.{{RESOURCE}}")}
        action={
          hasPermission("{{PERM}}:create") && (
            <Button onClick={openCreate}>{t("common.create")}</Button>
          )
        }
      />
      <QueryForm onSearch={handleSearch} onReset={handleReset}>
{{FILTER_FIELDS}}      </QueryForm>
      <DataTable
        columns={columns}
        rows={data?.content ?? []}
        loading={isLoading}
        rowKey="id"
        actions={(row) => (
          <>
            {hasPermission("{{PERM}}:update") && (
              <Button size="small" onClick={() => openEdit(row)}>
                {t("common.edit")}
              </Button>
            )}
            {hasPermission("{{PERM}}:delete") && (
              <ConfirmDialog
                title={t("common.confirmDelete")}
                onConfirm={() => remove(row.id)}
                trigger={
                  <Button size="small" color="error">
                    {t("common.delete")}
                  </Button>
                }
              />
            )}
          </>
        )}
      />
      <Pagination
        page={page}
        size={size}
        total={data?.totalElements ?? 0}
        onPageChange={setPage}
        onSizeChange={setSize}
      />
      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={editing ? t("common.edit") : t("common.create")}
      >
{{FORM_FIELDS}}        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>{t("common.cancel")}</Button>
          <Button variant="contained" onClick={handleSubmit}>
            {t("common.confirm")}
          </Button>
        </DialogActions>
      </Dialog>
    </PageStack>
  );
}
