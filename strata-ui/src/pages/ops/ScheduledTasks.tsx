// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, Drawer, Stack, Typography, Chip as MuiChip } from "@mui/material";
import { Button, PageHeader, DataTable, Chip } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import {
  opsScheduledTaskApi,
  type ScheduledTaskDto,
  type ScheduledTaskLogDto,
} from "@/shared/api/ops";

const STATUS_COLOR: Record<string, "success" | "default" | "error"> = {
  RUNNING: "success",
  PAUSED: "default",
  ERROR: "error",
};

function formatInstant(value: string | null): string {
  if (!value) return "—";
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value;
  }
}

export function ScheduledTasks() {
  const { t } = useI18n();
  const [logsFor, setLogsFor] = useState<ScheduledTaskDto | null>(null);

  const {
    data: tasks,
    loading,
    reload,
  } = useListQuery<ScheduledTaskDto>(["api", "ops", "scheduled-tasks"], () =>
    opsScheduledTaskApi.list(),
  );

  const { data: logs, loading: logsLoading } = useListQuery<ScheduledTaskLogDto>(
    ["api", "ops", "scheduled-tasks", logsFor?.id, "logs"],
    () => (logsFor ? opsScheduledTaskApi.logs(logsFor.id) : Promise.resolve([])),
  );

  const triggerMutation = useMutationWithToast(
    (id: string) => opsScheduledTaskApi.trigger(id),
    { success: "ops.scheduledTasks.triggered", failure: "toast.saveFailed" },
    { onSuccess: () => reload() },
  );

  const toggleMutation = useMutationWithToast(
    (id: string) => opsScheduledTaskApi.toggle(id),
    { success: "toast.saved", failure: "toast.saveFailed" },
    { onSuccess: () => reload() },
  );

  const cols = [
    { key: "name", title: t("common.name") },
    {
      key: "cron",
      title: t("ops.scheduledTasks.cron"),
      render: (r: ScheduledTaskDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.secondary">
          {r.cron}
        </Typography>
      ),
    },
    {
      key: "status",
      title: t("common.status"),
      render: (r: ScheduledTaskDto) => (
        <Chip
          label={r.status}
          size="small"
          color={STATUS_COLOR[r.status] ?? "default"}
          variant="outlined"
        />
      ),
    },
    {
      key: "lastRunTime",
      title: t("ops.scheduledTasks.lastRun"),
      render: (r: ScheduledTaskDto) => (
        <Typography variant="caption" color="text.disabled">
          {formatInstant(r.lastRunTime)}
        </Typography>
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: ScheduledTaskDto) => (
        <Stack direction="row" spacing={0.5}>
          <Button
            variant="text"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              void triggerMutation.mutateAsync(r.id);
            }}
          >
            {t("ops.scheduledTasks.trigger")}
          </Button>
          <Button
            variant="text"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              void toggleMutation.mutateAsync(r.id);
            }}
          >
            {r.status === "RUNNING"
              ? t("ops.scheduledTasks.pause")
              : t("ops.scheduledTasks.resume")}
          </Button>
          <Button
            variant="text"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              setLogsFor(r);
            }}
          >
            {t("ops.scheduledTasks.logs")}
          </Button>
        </Stack>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("ops.scheduledTasks.title")}
        subtitle={t("ops.scheduledTasks.subtitle")}
      />
      <DataTable
        columns={cols}
        data={tasks}
        loading={loading}
        rowKey={(r) => r.id}
        tableId="ops-scheduled-tasks"
      />

      <Drawer
        open={!!logsFor}
        onClose={() => setLogsFor(null)}
        anchor="right"
        sx={{ "& .MuiDrawer-paper": { width: 480 } }}
      >
        <Box sx={{ p: 3 }}>
          <Typography variant="h6" sx={{ mb: 2 }}>
            {t("ops.scheduledTasks.logs")}
            {logsFor ? ` · ${logsFor.name}` : ""}
          </Typography>
          {logsLoading ? (
            <Typography variant="body2" color="text.secondary">
              {t("common.loading")}
            </Typography>
          ) : logs.length === 0 ? (
            <Typography variant="body2" color="text.secondary">
              {t("common.noData")}
            </Typography>
          ) : (
            <Stack spacing={1.5}>
              {logs.map((log) => (
                <Box
                  key={log.id}
                  sx={{ border: 1, borderColor: "divider", borderRadius: 1, p: 1.5 }}
                >
                  <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 0.5 }}>
                    <MuiChip
                      label={log.status}
                      size="small"
                      color={STATUS_COLOR[log.status] ?? "default"}
                    />
                    <Typography variant="caption" color="text.secondary">
                      {formatInstant(log.startTime)}
                    </Typography>
                  </Stack>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    sx={{ whiteSpace: "pre-wrap" }}
                  >
                    {log.message || "—"}
                  </Typography>
                </Box>
              ))}
            </Stack>
          )}
        </Box>
      </Drawer>
    </PageStack>
  );
}

export default ScheduledTasks;
