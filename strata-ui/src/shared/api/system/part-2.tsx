// SPDX-License-Identifier: Apache-2.0
// Split out of system.ts by scripts/auto-split.mjs.

import { apiDelete, apiGet, apiPost, apiPut, toParams } from "@/shared/api/client";
import type { ApiKeyCreatedDto, ApiKeyDto, NotificationPrefDto, PageResult } from "@/shared/types";
import { ApiKeyCreatePayload } from "./part-1";

export interface ApiKeyUpdatePayload {
  name: string | null;
  scopes: string | null;
  enabled: boolean | null;
}

export type ApiKeySummary = { total: number; active: number };

export const apiKeyApi = {
  list: () => apiGet<ApiKeyDto[]>("/api/v1/system/api-keys"),
  page: (page: number, size: number) =>
    apiGet<PageResult<ApiKeyDto>>(`/api/v1/system/api-keys/page${toParams({ page, size })}`),
  summary: () => apiGet<ApiKeySummary>("/api/v1/system/api-keys/summary"),
  create: (payload: ApiKeyCreatePayload) =>
    apiPost<ApiKeyCreatedDto>("/api/v1/system/api-keys", payload),
  update: (id: string, payload: ApiKeyUpdatePayload) =>
    apiPut<ApiKeyDto>(`/api/v1/system/api-keys/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/api-keys/${id}`),
};

export const notificationPrefApi = {
  get: () => apiGet<NotificationPrefDto>("/api/v1/system/notification-preferences"),
  update: (payload: NotificationPrefDto) =>
    apiPut<NotificationPrefDto>("/api/v1/system/notification-preferences", payload),
};
