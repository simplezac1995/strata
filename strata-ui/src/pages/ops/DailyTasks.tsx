// SPDX-License-Identifier: Apache-2.0
import { DialogActions, Stack } from "@mui/material";
import {
  Button,
  TextField,
  Select,
  Dialog,
  ConfirmDialog,
  PageHeader,
  DataTable,
  Chip,
  Pagination,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import { opsDailyTaskApi, type DailyTaskDto, type DailyTaskPayload } from "@/shared/api/ops";

interface DailyTaskForm {
  name: string;
  type: string;
  targetCount: number;
  rewardPoints: number;
}

export function DailyTasks() {
  const { t } = useI18n();

  const { rows, total, page, setPage, pageSize, setPageSize, loading, error, reload } =
    usePagedQuery<DailyTaskDto>(["api", "ops", "daily-tasks"], opsDailyTaskApi.page);

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
  } = useResourceCrud<DailyTaskDto, DailyTaskPayload, DailyTaskPayload, DailyTaskForm>({
    create: (payload) => opsDailyTaskApi.create(payload),
    update: (id, payload) => opsDailyTaskApi.update(id, payload),
    remove: (id) => opsDailyTaskApi.remove(id),
    reload,
    initForm: () => ({ name: "", type: "CHECK_IN", targetCount: 1, rewardPoints: 0 }),
    toEditForm: (r) => ({
      name: r.name,
      type: r.type,
      targetCount: r.targetCount,
      rewardPoints: r.rewardPoints,
    }),
  });

  const cols = [
    { key: "name", title: t("common.name") },
    {
      key: "type",
      title: t("common.type"),
      render: (r: DailyTaskDto) => <Chip label={r.type} size="small" variant="outlined" />,
    },
    { key: "targetCount", title: t("ops.dailyTasks.targetCount") },
    { key: "rewardPoints", title: t("ops.dailyTasks.rewardPoints") },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: DailyTaskDto) => (
        <Stack direction="row" spacing={0.5}>
          <Button variant="text" size="sm" onClick={() => openEdit(r)}>
            {t("common.edit")}
          </Button>
          <Button variant="text" size="sm" color="danger" onClick={() => confirmDelete(r)}>
            {t("common.delete")}
          </Button>
        </Stack>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("ops.dailyTasks.title")}
        subtitle={t("ops.dailyTasks.subtitle")}
        actions={
          <Button variant="contained" onClick={openCreate}>
            {t("common.create")}
          </Button>
        }
      />
      <DataTable
        columns={cols}
        data={rows}
        loading={loading}
        rowKey={(r) => r.id}
        tableId="ops-daily-tasks"
        error={error}
        onRetry={reload}
        emptyAction={
          <Button variant="contained" onClick={openCreate}>
            {t("common.create")}
          </Button>
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
        title={edit ? t("common.edit") : t("common.create")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("common.name")}
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
          />
          <Select
            label={t("common.type")}
            value={form.type}
            onChange={(e) => setForm((f) => ({ ...f, type: e.target.value }))}
            options={[
              { value: "CHECK_IN", label: t("ops.dailyTasks.typeCheckIn") },
              { value: "SHARE", label: t("ops.dailyTasks.typeShare") },
              { value: "INVITE", label: t("ops.dailyTasks.typeInvite") },
              { value: "CONSUME", label: t("ops.dailyTasks.typeConsume") },
            ]}
          />
          <TextField
            label={t("ops.dailyTasks.targetCount")}
            type="number"
            value={form.targetCount}
            onChange={(e) => setForm((f) => ({ ...f, targetCount: Number(e.target.value) }))}
          />
          <TextField
            label={t("ops.dailyTasks.rewardPoints")}
            type="number"
            value={form.rewardPoints}
            onChange={(e) => setForm((f) => ({ ...f, rewardPoints: Number(e.target.value) }))}
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

export default DailyTasks;
