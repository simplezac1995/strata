// SPDX-License-Identifier: Apache-2.0
import { apiDelete, apiGet, apiPost } from "@/shared/api/client";
import client from "@/shared/api/client";
import type { FileInfoDto } from "@/shared/types";

export {
  userApi,
  roleApi,
  menuApi,
  deptApi,
  operationLogApi,
  auditLogApi,
  postApi,
  dictTypeApi,
  dictDataApi,
  paramApi,
  loginLogApi,
  tenantApi,
  clientApi,
  sensitiveWordApi,
  regionApi,
  webhookApi,
  emailTemplateApi,
  announcementApi,
  siteSettingApi,
  platformCapabilitiesApi,
  i18nEntryApi,
  jobApi,
  messageChannelApi,
  cacheApi,
} from "./system/part-1";
export type {
  UserCreatePayload,
  UserUpdatePayload,
  RoleCreatePayload,
  RoleUpdatePayload,
  MenuPayload,
  DeptPayload,
  PostPayload,
  DictTypeCreatePayload,
  DictTypeUpdatePayload,
  DictDataCreatePayload,
  DictDataUpdatePayload,
  ParamCreatePayload,
  ParamUpdatePayload,
  LoginLogQuery,
  TenantCreatePayload,
  TenantUpdatePayload,
  OauthClientCreatePayload,
  OauthClientUpdatePayload,
  RegionPayload,
  RegionUpdatePayload,
  WebhookPayload,
  EmailTemplateCreatePayload,
  EmailTemplateUpdatePayload,
  AnnouncementPayload,
  I18nCreatePayload,
  I18nUpdatePayload,
  JobCreatePayload,
  JobUpdatePayload,
  ApiKeyCreatePayload,
} from "./system/part-1";
export { apiKeyApi, notificationPrefApi } from "./system/part-2";
export type { ApiKeyUpdatePayload, ApiKeySummary } from "./system/part-2";

export const fileApi = {
  list: () => apiGet<FileInfoDto[]>("/api/v1/system/files"),
  upload: (file: File, bucket = "uploads") => {
    const form = new FormData();
    form.append("file", file);
    return apiPost<FileInfoDto>(`/api/v1/system/files?bucket=${encodeURIComponent(bucket)}`, form);
  },
  download: async (bucket: string, key: string): Promise<Blob> => {
    const { data } = await client.get<Blob>(
      `/api/v1/system/files/${bucket}/${encodeURIComponent(key)}/download`,
      { responseType: "blob" },
    );
    return data;
  },
  remove: (bucket: string, key: string) =>
    apiDelete<void>(`/api/v1/system/files/${bucket}/${encodeURIComponent(key)}`),
};
