// SPDX-License-Identifier: Apache-2.0
// Split out of system.ts by scripts/auto-split.mjs.

import { apiDelete, apiGet, apiPost, apiPut, toParams } from "@/shared/api/client";
import type {
  AnnouncementDto,
  CacheKeyDto,
  CacheStatusDto,
  DeptDto,
  DictDataDto,
  DictTypeDto,
  EmailTemplateDto,
  I18nEntryDto,
  JobDto,
  LoginLogDto,
  MenuDto,
  MenuType,
  MessageChannelDto,
  OauthClientDto,
  OperationLogDto,
  AuditLogDto,
  PageResult,
  ParamDto,
  PlatformCapabilitiesDto,
  PostDto,
  RegionDto,
  RoleDto,
  SensitiveWordDto,
  TenantDto,
  UserDto,
  WebhookDto,
} from "@/shared/types";

export interface UserCreatePayload {
  username: string;
  password: string;
  nickname: string | null;
  enabled: boolean;
  roleIds: string[];
}

export interface UserUpdatePayload {
  nickname: string | null;
  enabled: boolean;
  password: string | null;
  roleIds: string[];
}

export const userApi = {
  list: () => apiGet<UserDto[]>("/api/v1/system/users"),
  get: (id: string) => apiGet<UserDto>(`/api/v1/system/users/${id}`),
  /** Server-side paging (`GET /api/v1/system/users/page?keyword=&page=&size=`). */
  page: (keyword: string, page: number, size: number, sortBy?: string, sortDir?: string) =>
    apiGet<PageResult<UserDto>>(
      `/api/v1/system/users/page${toParams({ page, size, keyword, sortBy, sortDir })}`,
    ),
  exportUrl: (keyword?: string) => `/api/v1/system/users/export${toParams({ keyword })}`,
  batchDelete: (ids: string[]) =>
    apiPost<void>("/api/v1/system/users/batch-delete", { ids: ids.map(Number) }),
  create: (payload: UserCreatePayload) => apiPost<void>("/api/v1/system/users", payload),
  update: (id: string, payload: UserUpdatePayload) =>
    apiPut<void>(`/api/v1/system/users/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/users/${id}`),
};

export interface RoleCreatePayload {
  code: string;
  name: string;
  enabled: boolean;
  menuIds: string[];
}

export interface RoleUpdatePayload {
  name: string;
  enabled: boolean;
  menuIds: string[];
}

export const roleApi = {
  list: () => apiGet<RoleDto[]>("/api/v1/system/roles"),
  /** Server-side paging (`GET /api/v1/system/roles/page?keyword=&page=&size=`). */
  page: (keyword: string, page: number, size: number, sortBy?: string, sortDir?: string) =>
    apiGet<PageResult<RoleDto>>(
      `/api/v1/system/roles/page${toParams({ page, size, keyword, sortBy, sortDir })}`,
    ),
  create: (payload: RoleCreatePayload) => apiPost<void>("/api/v1/system/roles", payload),
  update: (id: string, payload: RoleUpdatePayload) =>
    apiPut<void>(`/api/v1/system/roles/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/roles/${id}`),
};

export interface MenuPayload {
  parentId: string | null;
  name: string;
  type: MenuType;
  path: string | null;
  component: string | null;
  permission: string | null;
  sortOrder: number;
  visible: boolean;
  enabled: boolean;
}

export const menuApi = {
  list: () => apiGet<MenuDto[]>("/api/v1/system/menus"),
  children: (parentId?: string) =>
    apiGet<MenuDto[]>(`/api/v1/system/menus/children${toParams({ parentId })}`),
  pageFlat: (keyword: string | undefined, page: number, size: number) =>
    apiGet<PageResult<MenuDto>>(`/api/v1/system/menus/flat${toParams({ page, size, keyword })}`),
  create: (payload: MenuPayload) => apiPost<void>("/api/v1/system/menus", payload),
  update: (id: string, payload: MenuPayload) => apiPut<void>(`/api/v1/system/menus/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/menus/${id}`),
};

export interface DeptPayload {
  parentId: string | null;
  name: string;
  sortOrder: number;
  enabled: boolean;
}

export const deptApi = {
  list: () => apiGet<DeptDto[]>("/api/v1/system/depts"),
  children: (parentId?: string) =>
    apiGet<DeptDto[]>(`/api/v1/system/depts/children${toParams({ parentId })}`),
  pageFlat: (keyword: string | undefined, page: number, size: number) =>
    apiGet<PageResult<DeptDto>>(`/api/v1/system/depts/flat${toParams({ page, size, keyword })}`),
  create: (payload: DeptPayload) => apiPost<void>("/api/v1/system/depts", payload),
  update: (id: string, payload: DeptPayload) => apiPut<void>(`/api/v1/system/depts/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/depts/${id}`),
};

export const operationLogApi = {
  page: (page: number, size: number) =>
    apiGet<PageResult<OperationLogDto>>(`/api/v1/system/operation-logs${toParams({ page, size })}`),
};

export const auditLogApi = {
  page: (page: number, size: number) =>
    apiGet<PageResult<AuditLogDto>>(`/api/v1/system/audit-logs${toParams({ page, size })}`),
};

export interface PostPayload {
  code: string;
  name: string;
  sortOrder: number;
  enabled: boolean;
}

export const postApi = {
  list: () => apiGet<PostDto[]>("/api/v1/system/posts"),
  page: (page: number, size: number) =>
    apiGet<PageResult<PostDto>>(`/api/v1/system/posts/page${toParams({ page, size })}`),
  create: (payload: PostPayload) => apiPost<void>("/api/v1/system/posts", payload),
  update: (id: string, payload: PostPayload) => apiPut<void>(`/api/v1/system/posts/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/posts/${id}`),
};

export interface DictTypeCreatePayload {
  code: string;
  name: string;
  sortOrder: number;
  enabled: boolean;
}

export interface DictTypeUpdatePayload {
  name: string;
  sortOrder: number;
  enabled: boolean;
}

export interface DictDataCreatePayload {
  typeId: string;
  value: string;
  label: string;
  sortOrder: number;
  enabled: boolean;
}

export interface DictDataUpdatePayload {
  label: string;
  sortOrder: number;
  enabled: boolean;
}

export const dictTypeApi = {
  list: () => apiGet<DictTypeDto[]>("/api/v1/system/dict-types"),
  create: (payload: DictTypeCreatePayload) => apiPost<void>("/api/v1/system/dict-types", payload),
  update: (id: string, payload: DictTypeUpdatePayload) =>
    apiPut<void>(`/api/v1/system/dict-types/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/dict-types/${id}`),
};

export const dictDataApi = {
  listByType: (typeId: string) =>
    apiGet<DictDataDto[]>(`/api/v1/system/dict-data${toParams({ typeId })}`),
  create: (payload: DictDataCreatePayload) => apiPost<void>("/api/v1/system/dict-data", payload),
  update: (id: string, payload: DictDataUpdatePayload) =>
    apiPut<void>(`/api/v1/system/dict-data/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/dict-data/${id}`),
};

export interface ParamCreatePayload {
  key: string;
  value: string;
  type: string;
  remark: string | null;
}

export interface ParamUpdatePayload {
  value: string;
  type: string;
  remark: string | null;
}

export const paramApi = {
  list: () => apiGet<ParamDto[]>("/api/v1/system/params"),
  page: (keyword: string, page: number, size: number) =>
    apiGet<PageResult<ParamDto>>(`/api/v1/system/params/page${toParams({ page, size, keyword })}`),
  create: (payload: ParamCreatePayload) => apiPost<void>("/api/v1/system/params", payload),
  update: (id: string, payload: ParamUpdatePayload) =>
    apiPut<void>(`/api/v1/system/params/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/params/${id}`),
};

export interface LoginLogQuery {
  username?: string;
  status?: string;
  ip?: string;
  from?: string;
  to?: string;
  page: number;
  size: number;
}

export const loginLogApi = {
  page: (query: LoginLogQuery) =>
    apiGet<PageResult<LoginLogDto>>(
      `/api/v1/system/login-logs${toParams({
        page: query.page,
        size: query.size,
        username: query.username,
        status: query.status,
        ip: query.ip,
        from: query.from,
        to: query.to,
      })}`,
    ),
};

export interface TenantCreatePayload {
  code: string;
  name: string;
  domain: string | null;
  status: string;
  plan: string;
}

export interface TenantUpdatePayload {
  name: string;
  domain: string | null;
  status: string;
  plan: string;
}

export interface OauthClientCreatePayload {
  clientId: string;
  name: string;
  clientSecret: string;
  scopes: string | null;
  enabled: boolean;
}

export interface OauthClientUpdatePayload {
  name: string;
  scopes: string | null;
  enabled: boolean;
}

export const tenantApi = {
  list: () => apiGet<TenantDto[]>("/api/v1/system/tenants"),
  page: (page: number, size: number) =>
    apiGet<PageResult<TenantDto>>(`/api/v1/system/tenants/page${toParams({ page, size })}`),
  create: (payload: TenantCreatePayload) => apiPost<void>("/api/v1/system/tenants", payload),
  update: (id: string, payload: TenantUpdatePayload) =>
    apiPut<void>(`/api/v1/system/tenants/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/tenants/${id}`),
};

export const clientApi = {
  list: () => apiGet<OauthClientDto[]>("/api/v1/system/clients"),
  page: (page: number, size: number) =>
    apiGet<PageResult<OauthClientDto>>(`/api/v1/system/clients/page${toParams({ page, size })}`),
  create: (payload: OauthClientCreatePayload) => apiPost<void>("/api/v1/system/clients", payload),
  update: (id: string, payload: OauthClientUpdatePayload) =>
    apiPut<void>(`/api/v1/system/clients/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/clients/${id}`),
};

export const sensitiveWordApi = {
  list: () => apiGet<SensitiveWordDto[]>("/api/v1/system/sensitive-words"),
  page: (category: "sensitive" | "whitelist" | undefined, page: number, size: number) =>
    apiGet<PageResult<SensitiveWordDto>>(
      `/api/v1/system/sensitive-words/page${toParams({ page, size, category })}`,
    ),
  create: (word: string) =>
    apiPost<void>("/api/v1/system/sensitive-words", { word, enabled: true }),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/sensitive-words/${id}`),
};

export interface RegionPayload {
  code: string;
  name: string;
  level: number;
  parentId: string | null;
}

export interface RegionUpdatePayload {
  name: string;
  level: number;
  parentId: string | null;
}

export const regionApi = {
  list: () => apiGet<RegionDto[]>("/api/v1/system/regions"),
  page: (page: number, size: number) =>
    apiGet<PageResult<RegionDto>>(`/api/v1/system/regions/page${toParams({ page, size })}`),
  create: (payload: RegionPayload) => apiPost<void>("/api/v1/system/regions", payload),
  update: (id: string, payload: RegionUpdatePayload) =>
    apiPut<void>(`/api/v1/system/regions/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/regions/${id}`),
};

export interface WebhookPayload {
  name: string;
  url: string;
  secret: string | null;
  enabled: boolean;
}

export interface EmailTemplateCreatePayload {
  code: string;
  name: string;
  subject: string;
  body: string | null;
  enabled: boolean;
}

export interface EmailTemplateUpdatePayload {
  name: string;
  subject: string;
  body: string | null;
  enabled: boolean;
}

export interface AnnouncementPayload {
  title: string;
  content: string | null;
  enabled: boolean;
}

export const webhookApi = {
  list: () => apiGet<WebhookDto[]>("/api/v1/system/webhooks"),
  page: (page: number, size: number) =>
    apiGet<PageResult<WebhookDto>>(`/api/v1/system/webhooks/page${toParams({ page, size })}`),
  create: (payload: WebhookPayload) => apiPost<void>("/api/v1/system/webhooks", payload),
  update: (id: string, payload: WebhookPayload) =>
    apiPut<void>(`/api/v1/system/webhooks/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/webhooks/${id}`),
};

export const emailTemplateApi = {
  list: () => apiGet<EmailTemplateDto[]>("/api/v1/system/email-templates"),
  page: (page: number, size: number) =>
    apiGet<PageResult<EmailTemplateDto>>(
      `/api/v1/system/email-templates/page${toParams({ page, size })}`,
    ),
  create: (payload: EmailTemplateCreatePayload) =>
    apiPost<void>("/api/v1/system/email-templates", payload),
  update: (id: string, payload: EmailTemplateUpdatePayload) =>
    apiPut<void>(`/api/v1/system/email-templates/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/email-templates/${id}`),
};

export const announcementApi = {
  list: () => apiGet<AnnouncementDto[]>("/api/v1/system/announcements"),
  page: (page: number, size: number) =>
    apiGet<PageResult<AnnouncementDto>>(
      `/api/v1/system/announcements/page${toParams({ page, size })}`,
    ),
  create: (payload: AnnouncementPayload) => apiPost<void>("/api/v1/system/announcements", payload),
  update: (id: string, payload: AnnouncementPayload) =>
    apiPut<void>(`/api/v1/system/announcements/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/announcements/${id}`),
};

export const siteSettingApi = {
  list: () => apiGet<Record<string, string>>("/api/v1/system/site-settings"),
  putAll: (body: Record<string, string>) =>
    apiPut<Record<string, string>>("/api/v1/system/site-settings", body),
};

export const platformCapabilitiesApi = {
  get: () => apiGet<PlatformCapabilitiesDto>("/api/v1/system/capabilities"),
};

export interface I18nCreatePayload {
  messageKey: string;
  zh: string | null;
  en: string | null;
}

export interface I18nUpdatePayload {
  zh: string | null;
  en: string | null;
}

export const i18nEntryApi = {
  page: (keyword: string, page: number, size: number) =>
    apiGet<PageResult<I18nEntryDto>>(`/api/v1/system/i18n${toParams({ page, size, keyword })}`),
  create: (payload: I18nCreatePayload) => apiPost<I18nEntryDto>("/api/v1/system/i18n", payload),
  update: (id: string, payload: I18nUpdatePayload) =>
    apiPut<I18nEntryDto>(`/api/v1/system/i18n/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/i18n/${id}`),
};

export interface JobCreatePayload {
  name: string;
  jobGroup: string | null;
  cron: string;
  enabled: boolean | null;
}

export interface JobUpdatePayload {
  name: string | null;
  jobGroup: string | null;
  cron: string | null;
  enabled: boolean | null;
}

export const jobApi = {
  list: () => apiGet<JobDto[]>("/api/v1/system/jobs"),
  page: (page: number, size: number) =>
    apiGet<PageResult<JobDto>>(`/api/v1/system/jobs/page${toParams({ page, size })}`),
  create: (payload: JobCreatePayload) => apiPost<JobDto>("/api/v1/system/jobs", payload),
  update: (id: string, payload: JobUpdatePayload) =>
    apiPut<JobDto>(`/api/v1/system/jobs/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/system/jobs/${id}`),
  pause: (id: string) => apiPost<JobDto>(`/api/v1/system/jobs/${id}/pause`),
  run: (id: string) => apiPost<JobDto>(`/api/v1/system/jobs/${id}/run`),
};

export const messageChannelApi = {
  list: () => apiGet<MessageChannelDto[]>("/api/v1/system/message-channels"),
  page: (page: number, size: number) =>
    apiGet<PageResult<MessageChannelDto>>(
      `/api/v1/system/message-channels/page${toParams({ page, size })}`,
    ),
  update: (channelType: string, configJson: string, enabled: boolean) =>
    apiPut<MessageChannelDto>(`/api/v1/system/message-channels/${channelType}`, {
      configJson,
      enabled,
    }),
};

export const cacheApi = {
  status: () => apiGet<CacheStatusDto>("/api/v1/system/cache/status"),
  keys: (pattern?: string) => {
    const q = pattern ? `?pattern=${encodeURIComponent(pattern)}` : "";
    return apiGet<CacheKeyDto[]>(`/api/v1/system/cache/keys${q}`);
  },
  deleteKey: (key: string) =>
    apiDelete<void>(`/api/v1/system/cache/keys?key=${encodeURIComponent(key)}`),
  flush: () => apiDelete<void>("/api/v1/system/cache/flush"),
};

export interface ApiKeyCreatePayload {
  name: string;
  scopes: string | null;
  enabled: boolean | null;
}
