// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Badge,
  Box,
  Button,
  Divider,
  IconButton,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Menu,
  Stack,
  Tooltip,
  Typography,
} from "@mui/material";
import NotificationsIcon from "@mui/icons-material/Notifications";
import NotificationsNoneIcon from "@mui/icons-material/NotificationsNone";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import ErrorIcon from "@mui/icons-material/Error";
import InfoIcon from "@mui/icons-material/Info";
import WarningIcon from "@mui/icons-material/Warning";
import DoneAllIcon from "@mui/icons-material/DoneAll";
import { useWebSocket } from "@/shared/websocket";
import { useI18n } from "@/shared/i18n";
import { notificationApi, type NotificationDto } from "@/shared/api/notifications";

const TYPE_CONFIG = {
  info: { icon: InfoIcon, color: "primary.main", bg: "primary.lighter" },
  success: { icon: CheckCircleIcon, color: "success.main", bg: "success.lighter" },
  warning: { icon: WarningIcon, color: "warning.main", bg: "warning.lighter" },
  error: { icon: ErrorIcon, color: "error.main", bg: "error.lighter" },
} as const;

function formatRelative(timestamp: string, t: (key: string) => string): string {
  const date = new Date(timestamp);
  const diffMs = Date.now() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);
  const withCount = (template: string, value: number) => template.replace("{n}", String(value));
  if (diffMins < 1) return t("notifications.relative.justNow");
  if (diffMins < 60) return withCount(t("notifications.relative.minutesAgo"), diffMins);
  if (diffHours < 24) return withCount(t("notifications.relative.hoursAgo"), diffHours);
  if (diffDays < 7) return withCount(t("notifications.relative.daysAgo"), diffDays);
  return date.toLocaleDateString();
}

function RelativeTime({ ts }: { ts: string }) {
  const { t } = useI18n();
  const [, tick] = useState(0);
  useEffect(() => {
    const id = setInterval(() => tick((v) => v + 1), 60_000);
    return () => clearInterval(id);
  }, []);
  return (
    <Typography variant="caption" color="text.secondary">
      {formatRelative(ts, t)}
    </Typography>
  );
}

export function NotificationCenter() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const { subscribe, connected } = useWebSocket({
    autoReconnect: true,
    debug: false,
  });

  const { data: page } = useQuery({
    queryKey: ["api", "notifications", "list", { onlyUnread: true, page: 1, size: 20 }],
    queryFn: () => notificationApi.page(true, 1, 20),
  });
  const { data: count } = useQuery({
    queryKey: ["api", "notifications", "unread-count"],
    queryFn: () => notificationApi.unreadCount(),
    refetchInterval: 30_000,
  });

  const items = page?.content ?? [];
  const unreadCount = count?.count ?? items.filter((n) => !n.read).length;

  const markReadMutation = useMutation({
    mutationFn: (id: string) => notificationApi.markRead(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["api", "notifications"] });
    },
  });
  const markAllMutation = useMutation({
    mutationFn: () => notificationApi.markAllRead(),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["api", "notifications"] });
    },
  });

  useEffect(() => {
    const unsubscribe = subscribe("notification", () => {
      void queryClient.invalidateQueries({ queryKey: ["api", "notifications"] });
    });
    return unsubscribe;
  }, [subscribe, queryClient]);

  const menuId = "notification-menu";
  const isMenuOpen = Boolean(anchorEl);

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };
  const handleMenuClose = () => setAnchorEl(null);

  const handleItemClick = (n: NotificationDto) => {
    if (!n.read) markReadMutation.mutate(n.id);
    if (n.actionUrl) {
      handleMenuClose();
      navigate(n.actionUrl);
    }
  };

  const handleMarkAll = () => markAllMutation.mutate();

  const goFullPage = () => {
    handleMenuClose();
    navigate("/notifications");
  };

  return (
    <>
      <Tooltip title={connected ? t("notifications.title") : t("notifications.titleDisconnected")}>
        <IconButton
          onClick={handleMenuOpen}
          color="inherit"
          aria-label={t("notifications.title")}
          aria-controls={isMenuOpen ? menuId : undefined}
          aria-haspopup="true"
          aria-expanded={isMenuOpen ? "true" : undefined}
        >
          <Badge badgeContent={unreadCount} color="error" max={99}>
            {unreadCount > 0 ? <NotificationsIcon /> : <NotificationsNoneIcon />}
          </Badge>
        </IconButton>
      </Tooltip>

      <Menu
        anchorEl={anchorEl}
        id={menuId}
        keepMounted
        open={isMenuOpen}
        onClose={handleMenuClose}
        transformOrigin={{ horizontal: "right", vertical: "top" }}
        anchorOrigin={{ horizontal: "right", vertical: "bottom" }}
        slotProps={{
          paper: {
            sx: {
              width: 380,
              maxHeight: 480,
              overflow: "hidden",
            },
          },
        }}
      >
        <Box
          sx={{
            px: 2,
            py: 1.5,
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
          }}
        >
          <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
              {t("notifications.title")}
            </Typography>
            {unreadCount > 0 && (
              <Badge
                badgeContent={unreadCount}
                color="error"
                sx={{ "& .MuiBadge-badge": { fontSize: 10 } }}
              >
                <Box sx={{ width: 0, height: 0 }} />
              </Badge>
            )}
          </Stack>
          <Tooltip title={t("notifications.markAllAsRead")}>
            <IconButton
              size="small"
              aria-label={t("notifications.markAllAsRead")}
              onClick={handleMarkAll}
              disabled={unreadCount === 0}
            >
              <DoneAllIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Box>

        <Divider />

        <Box sx={{ maxHeight: 360, overflowY: "auto" }}>
          {items.length === 0 ? (
            <Box sx={{ py: 6, textAlign: "center" }}>
              <NotificationsNoneIcon sx={{ fontSize: 48, color: "text.disabled", mb: 1 }} />
              <Typography variant="body2" color="text.secondary">
                {t("notifications.noNotifications")}
              </Typography>
              {!connected && (
                <Typography
                  variant="caption"
                  color="text.disabled"
                  sx={{ mt: 0.5, display: "block" }}
                >
                  {t("notifications.wsDisconnectedHint")}
                </Typography>
              )}
            </Box>
          ) : (
            <List disablePadding>
              {items.map((notification) => {
                const config = TYPE_CONFIG[notification.type] ?? TYPE_CONFIG.info;
                const Icon = config.icon;
                return (
                  <ListItem
                    key={notification.id}
                    alignItems="flex-start"
                    onClick={() => handleItemClick(notification)}
                    sx={{
                      px: 2,
                      py: 1.5,
                      cursor: "pointer",
                      bgcolor: notification.read ? "transparent" : config.bg,
                      borderBottom: "1px solid",
                      borderColor: "divider",
                      "&:hover": {
                        bgcolor: notification.read ? "action.hover" : config.bg,
                      },
                    }}
                  >
                    <ListItemAvatar sx={{ minWidth: 36 }}>
                      <Icon sx={{ color: config.color, fontSize: 20 }} />
                    </ListItemAvatar>
                    <ListItemText
                      primary={
                        <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 0.25 }}>
                          <Typography
                            variant="body2"
                            sx={{ fontWeight: notification.read ? 400 : 600 }}
                          >
                            {notification.title}
                          </Typography>
                          <RelativeTime ts={notification.createdAt} />
                        </Stack>
                      }
                      secondary={
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
                          {notification.content}
                        </Typography>
                      }
                    />
                  </ListItem>
                );
              })}
            </List>
          )}
        </Box>

        {items.length > 0 && (
          <>
            <Divider />
            <Box sx={{ p: 1, textAlign: "center" }}>
              <Button size="small" onClick={goFullPage}>
                {t("notifications.viewAll")}
              </Button>
            </Box>
          </>
        )}
      </Menu>
    </>
  );
}
