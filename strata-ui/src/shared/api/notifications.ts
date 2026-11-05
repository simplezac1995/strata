// SPDX-License-Identifier: Apache-2.0
import { apiGet, apiPost, apiPut, toParams } from "@/shared/api/client";
import type { PageResult } from "@/shared/types";

/* ─── In-app notifications (contract-aligned, do not guess fields) ─────────── */

export type NotificationType = "info" | "success" | "warning" | "error";
export type NotificationCategory = "system" | "security" | "business";

export interface NotificationDto {
  id: string;
  title: string;
  content: string;
  type: NotificationType;
  category: NotificationCategory;
  read: boolean;
  actionUrl: string | null;
  createdAt: string;
}

export interface NotificationBroadcastPayload {
  title: string;
  content: string;
  type?: NotificationType;
  category?: NotificationCategory;
  actionUrl?: string | null;
}

export const notificationApi = {
  page: (onlyUnread: boolean, page: number, size: number) =>
    apiGet<PageResult<NotificationDto>>(
      `/api/v1/system/notifications${toParams({ onlyUnread, page, size })}`,
    ),
  unreadCount: () => apiGet<{ count: number }>("/api/v1/system/notifications/unread-count"),
  markRead: (id: string) => apiPut<void>(`/api/v1/system/notifications/${id}/read`),
  markAllRead: () => apiPut<void>("/api/v1/system/notifications/read-all"),
  broadcast: (payload: NotificationBroadcastPayload) =>
    apiPost<void>("/api/v1/system/notifications/broadcast", payload),
};
