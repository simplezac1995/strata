// SPDX-License-Identifier: Apache-2.0
import { apiDelete, apiGet, apiPost, toParams } from "@/shared/api/client";
import client from "@/shared/api/client";
import type {
  FileInfoDto,
  LoginLogDto,
  OperationLogDto,
  AuditLogDto,
  PageResult,
} from "@/shared/types";

/* ─── File management center (#3) ───────────────────────────── */

export interface FileRef {
  bucket: string;
  key: string;
}

/**
 * File center API surface. The backend FileController list endpoint returns the
 * full collection (no server-side paging), so the page does local filtering /
 * pagination. Upload keeps the default `uploads` bucket.
 */
export const fileCenterApi = {
  list: (): Promise<FileInfoDto[]> => apiGet<FileInfoDto[]>("/api/v1/system/files"),

  upload: (file: File, bucket = "uploads"): Promise<FileInfoDto> => {
    const form = new FormData();
    form.append("file", file);
    return apiPost<FileInfoDto>(`/api/v1/system/files?bucket=${encodeURIComponent(bucket)}`, form);
  },

  remove: (bucket: string, key: string): Promise<void> =>
    apiDelete<void>(`/api/v1/system/files/${bucket}/${encodeURIComponent(key)}`),

  downloadBlob: async (bucket: string, key: string): Promise<Blob> => {
    const { data } = await client.get<Blob>(
      `/api/v1/system/files/${bucket}/${encodeURIComponent(key)}/download`,
      { responseType: "blob" },
    );
    return data;
  },

  /**
   * Batch delete. The backend exposes no dedicated batch endpoint yet, so we fan
   * out individual delete calls and settle once all finish (best-effort: failures
   * surface through the per-call toast path).
   */
  batchRemove: (items: FileRef[]): Promise<void[]> =>
    Promise.all(items.map((it) => fileCenterApi.remove(it.bucket, it.key))),
};

/* ─── Log query filtering (#7) ────────────────────────────────── */

export interface OperationLogQuery {
  page: number;
  size: number;
  /** Fuzzy operator username (reserved; backend may ignore until wired). */
  username?: string;
  /** Module name filter (reserved). */
  module?: string;
  /** Action type filter (reserved). */
  action?: string;
  /** ISO datetime lower bound (reserved). */
  from?: string;
  /** ISO datetime upper bound (reserved). */
  to?: string;
}

export const operationLogQueryApi = {
  page: (q: OperationLogQuery): Promise<PageResult<OperationLogDto>> =>
    apiGet<PageResult<OperationLogDto>>(
      `/api/v1/system/operation-logs${toParams({
        page: q.page,
        size: q.size,
        username: q.username,
        module: q.module,
        action: q.action,
        from: q.from,
        to: q.to,
      })}`,
    ),
};

export interface LoginLogQuery {
  page: number;
  size: number;
  username?: string;
  status?: string;
  ip?: string;
  from?: string;
  to?: string;
}

export const loginLogQueryApi = {
  page: (q: LoginLogQuery): Promise<PageResult<LoginLogDto>> =>
    apiGet<PageResult<LoginLogDto>>(
      `/api/v1/system/login-logs${toParams({
        page: q.page,
        size: q.size,
        username: q.username,
        status: q.status,
        ip: q.ip,
        from: q.from,
        to: q.to,
      })}`,
    ),
};

export interface AuditLogQuery {
  page: number;
  size: number;
  /** Operator username (reserved; backend may ignore until wired). */
  operator?: string;
  from?: string;
  to?: string;
}

export const auditLogQueryApi = {
  page: (q: AuditLogQuery): Promise<PageResult<AuditLogDto>> =>
    apiGet<PageResult<AuditLogDto>>(
      `/api/v1/system/audit-logs${toParams({
        page: q.page,
        size: q.size,
        operator: q.operator,
        from: q.from,
        to: q.to,
      })}`,
    ),
};
