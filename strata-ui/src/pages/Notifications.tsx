// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Box, Chip, Stack, Typography } from "@mui/material";
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined";
import DoneAllIcon from "@mui/icons-material/DoneAll";
import { Button, DataTable, PageHeader, Pagination, Tabs } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { notificationApi, type NotificationDto } from "@/shared/api/notifications";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { PageWorkspace } from "@/shared/pageKit/PageWorkspace";

type TabId = "all" | "unread" | "system" | "security" | "business";

const TYPE_COLOR: Record<
  NotificationDto["type"],
  "default" | "info" | "success" | "warning" | "error"
> = {
  info: "info",
  success: "success",
  warning: "warning",
  error: "error",
};

export function Notifications() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>("all");

  const onlyUnread = tab === "unread";
  const categoryFilter = tab === "system" || tab === "security" || tab === "business" ? tab : "";

  const { rows, total, page, setPage, pageSize, setPageSize, loading, error, reload } =
    usePagedQuery<NotificationDto>(
      ["api", "notifications", "page", { tab, onlyUnread }],
      (pageIndex, size) => notificationApi.page(onlyUnread, pageIndex, size),
      { initialPageSize: 20 },
    );

  const markAllMutation = useMutation({
    mutationFn: () => notificationApi.markAllRead(),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["api", "notifications"] });
    },
  });

  const markReadMutation = useMutation({
    mutationFn: (id: string) => notificationApi.markRead(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["api", "notifications"] });
    },
  });

  const handleRowClick = (row: NotificationDto) => {
    if (!row.read) markReadMutation.mutate(row.id);
    if (row.actionUrl) navigate(row.actionUrl);
  };

  const filteredRows = categoryFilter ? rows.filter((r) => r.category === categoryFilter) : rows;

  const cols = [
    {
      key: "type",
      title: t("notifications.col.type"),
      width: 100,
      render: (r: NotificationDto) => (
        <Chip label={r.type} color={TYPE_COLOR[r.type] ?? "default"} size="small" />
      ),
    },
    {
      key: "title",
      title: t("notifications.col.title"),
      render: (r: NotificationDto) => (
        <Typography variant="body2" sx={{ fontWeight: r.read ? 400 : 600 }}>
          {r.title}
        </Typography>
      ),
    },
    {
      key: "content",
      title: t("notifications.col.content"),
      render: (r: NotificationDto) => (
        <Typography
          variant="body2"
          color="text.secondary"
          sx={{
            display: "-webkit-box",
            WebkitLineClamp: 2,
            WebkitBoxOrient: "vertical",
            overflow: "hidden",
          }}
        >
          {r.content}
        </Typography>
      ),
    },
    {
      key: "category",
      title: t("notifications.col.category"),
      width: 100,
      render: (r: NotificationDto) => <Chip label={r.category} size="small" variant="outlined" />,
    },
    {
      key: "createdAt",
      title: t("notifications.col.createdAt"),
      width: 180,
      render: (r: NotificationDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.secondary">
          {new Date(r.createdAt).toLocaleString()}
        </Typography>
      ),
    },
  ];

  return (
    <PageWorkspace>
      <Stack spacing={2}>
        <PageHeader
          title={
            <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
              <NotificationsNoneOutlinedIcon color="primary" sx={{ fontSize: 28 }} />
              <span>{t("nav.notifications")}</span>
            </Stack>
          }
          subtitle={t("notifications.pageSubtitle")}
          actions={
            <Button
              variant="outlined"
              startIcon={<DoneAllIcon />}
              onClick={() => markAllMutation.mutate()}
              disabled={markAllMutation.isPending}
            >
              {t("notifications.markAllAsRead")}
            </Button>
          }
        />

        <Box
          sx={{ bgcolor: "background.paper", border: 1, borderColor: "divider", borderRadius: 2 }}
        >
          <Tabs
            active={tab}
            onChange={(id) => {
              setTab(id as TabId);
              setPage(1);
            }}
            tabs={[
              { id: "all", label: t("notifications.tab.all") },
              { id: "unread", label: t("notifications.tab.unread") },
              { id: "system", label: t("notifications.tab.system") },
              { id: "security", label: t("notifications.tab.security") },
              { id: "business", label: t("notifications.tab.business") },
            ]}
          />
          <DataTable
            columns={cols}
            data={filteredRows}
            loading={loading}
            rowKey={(r) => r.id}
            tableId="notifications.list"
            error={error}
            onRetry={reload}
            onRowClick={handleRowClick}
          />
          <Pagination
            page={page}
            pageSize={pageSize}
            total={total}
            onPage={setPage}
            onPageSize={setPageSize}
          />
        </Box>
      </Stack>
    </PageWorkspace>
  );
}

export default Notifications;
