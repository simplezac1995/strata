// SPDX-License-Identifier: Apache-2.0
import { apiDelete, apiGet, apiPost, apiPut, toParams } from "@/shared/api/client";
import type { PageResult } from "@/shared/types";

/* ─── Ops module: announcements / popups / sign-in / daily tasks ────────── */

export interface AnnouncementDto {
  id: string;
  title: string;
  content: string | null;
  type: string;
  startAt: string | null;
  endAt: string | null;
  status: string;
  sort: number;
}

export interface AnnouncementPayload {
  title: string;
  content: string | null;
  type: string;
  startAt: string | null;
  endAt: string | null;
  status: string;
  sort: number;
}

export const opsAnnouncementApi = {
  /** Server-side paging: `GET /api/v1/ops/announcements?page=&size=`. */
  page: (page: number, size: number) =>
    apiGet<PageResult<AnnouncementDto>>(`/api/v1/ops/announcements${toParams({ page, size })}`),
  active: () => apiGet<AnnouncementDto[]>("/api/v1/ops/announcements/active"),
  create: (payload: AnnouncementPayload) => apiPost<void>("/api/v1/ops/announcements", payload),
  update: (id: string, payload: AnnouncementPayload) =>
    apiPut<void>(`/api/v1/ops/announcements/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/ops/announcements/${id}`),
};

/* ─── Popups ─────────────────────────────────────────────────────────── */

export interface PopupDto {
  id: string;
  name: string;
  imageUrl: string | null;
  linkUrl: string | null;
  startAt: string | null;
  endAt: string | null;
  frequency: string;
  enabled: boolean;
}

export interface PopupPayload {
  name: string;
  imageUrl: string | null;
  linkUrl: string | null;
  startAt: string | null;
  endAt: string | null;
  frequency: string;
  enabled: boolean;
}

export const opsPopupApi = {
  page: (page: number, size: number) =>
    apiGet<PageResult<PopupDto>>(`/api/v1/ops/popups${toParams({ page, size })}`),
  create: (payload: PopupPayload) => apiPost<void>("/api/v1/ops/popups", payload),
  update: (id: string, payload: PopupPayload) => apiPut<void>(`/api/v1/ops/popups/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/ops/popups/${id}`),
};

/* ─── Sign-in ────────────────────────────────────────────────────────── */

export interface SignInDayConfigDto {
  day: number;
  rewardPoints: number;
  rewardMinor: number;
}

export interface SignInRecordDto {
  id: string;
  username: string;
  signInDate: string;
  day: number;
  rewardPoints: number;
  rewardMinor: number;
}

export interface SignInCheckinResultDto {
  day: number;
  rewardPoints: number;
  rewardMinor: number;
}

export const opsSignInApi = {
  getConfig: () => apiGet<SignInDayConfigDto[]>("/api/v1/ops/signin/config"),
  saveConfig: (config: SignInDayConfigDto[]) =>
    apiPut<SignInDayConfigDto[]>("/api/v1/ops/signin/config", config),
  records: (page: number, size: number) =>
    apiGet<PageResult<SignInRecordDto>>(`/api/v1/ops/signin/records${toParams({ page, size })}`),
  checkin: () => apiPost<SignInCheckinResultDto>("/api/v1/ops/signin/checkin"),
};

/* ─── Daily tasks ────────────────────────────────────────────────────── */

export interface DailyTaskDto {
  id: string;
  name: string;
  type: string;
  targetCount: number;
  rewardPoints: number;
}

export interface DailyTaskPayload {
  name: string;
  type: string;
  targetCount: number;
  rewardPoints: number;
}

export const opsDailyTaskApi = {
  page: (page: number, size: number) =>
    apiGet<PageResult<DailyTaskDto>>(`/api/v1/ops/daily-tasks${toParams({ page, size })}`),
  create: (payload: DailyTaskPayload) => apiPost<void>("/api/v1/ops/daily-tasks", payload),
  update: (id: string, payload: DailyTaskPayload) =>
    apiPut<void>(`/api/v1/ops/daily-tasks/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/ops/daily-tasks/${id}`),
};

/* ─── API stats ─────────────────────────────────────────────────────── */

export interface ApiStatsSummaryDto {
  totalCalls: number;
  avgTimeMs: number;
  errorRate: number;
}

export interface ApiStatRowDto {
  path: string;
  calls: number;
  avgTimeMs: number;
  errorRate: number;
}

export interface ApiStatsDto {
  summary: ApiStatsSummaryDto;
  items: ApiStatRowDto[];
}

export const opsApiStatsApi = {
  stats: (days = 7) => apiGet<ApiStatsDto>(`/api/v1/ops/api-stats${toParams({ days })}`),
};

/* ─── Health check ───────────────────────────────────────────────────── */

export type HealthState = "up" | "down" | "unknown";

export interface ComponentHealthDto {
  status: HealthState;
  responseTimeMs?: number;
  usage?: number;
  message?: string;
}

export interface HealthCheckDto {
  db: ComponentHealthDto;
  redis: ComponentHealthDto;
  disk: ComponentHealthDto;
  cpu: ComponentHealthDto;
  memory: ComponentHealthDto;
  uptimeMs: number;
}

export const opsHealthApi = {
  check: () => apiGet<HealthCheckDto>("/api/v1/ops/health/check"),
};

/* ─── Scheduled tasks ────────────────────────────────────────────────── */

export interface ScheduledTaskDto {
  id: string;
  name: string;
  cron: string;
  status: string;
  lastRunTime: string | null;
}

export interface ScheduledTaskLogDto {
  id: string;
  taskId: string;
  startTime: string | null;
  endTime: string | null;
  status: string;
  message: string | null;
}

export const opsScheduledTaskApi = {
  list: () => apiGet<ScheduledTaskDto[]>("/api/v1/ops/scheduled-tasks"),
  trigger: (id: string) => apiPost<void>(`/api/v1/ops/scheduled-tasks/${id}/trigger`),
  toggle: (id: string) => apiPost<void>(`/api/v1/ops/scheduled-tasks/${id}/toggle`),
  logs: (id: string) => apiGet<ScheduledTaskLogDto[]>(`/api/v1/ops/scheduled-tasks/${id}/logs`),
};

/* ─── Backups ───────────────────────────────────────────────────────── */

export interface BackupDto {
  id: string;
  fileName: string;
  size: number;
  type: string;
  status: string;
  createdAt: string | null;
}

export const opsBackupApi = {
  page: (page: number, size: number) =>
    apiGet<PageResult<BackupDto>>(`/api/v1/ops/backups${toParams({ page, size })}`),
  create: () => apiPost<BackupDto>("/api/v1/ops/backups"),
  remove: (id: string) => apiDelete<void>(`/api/v1/ops/backups/${id}`),
  restore: (id: string) => apiPost<void>(`/api/v1/ops/backups/${id}/restore`),
};
