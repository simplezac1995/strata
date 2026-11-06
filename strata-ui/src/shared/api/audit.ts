// SPDX-License-Identifier: Apache-2.0
import { apiGet, apiPost, toParams } from "@/shared/api/client";
import type { PageResult } from "@/shared/types";

/* ─── Audit log changes DTOs (contract-aligned, do not guess fields) ───── */

export interface AuditLogChangeDto {
  id: string;
  operationLogId: string;
  entityName: string;
  entityId: string;
  fieldName: string;
  oldValue: string;
  newValue: string;
  createdAt: string;
}

export interface AuditChangePageQuery {
  entity?: string;
  entityId?: string;
}

const BASE = "/api/v1/audit/changes";

export const auditApi = {
  changesPage: (query: AuditChangePageQuery, page: number, size: number) =>
    apiGet<PageResult<AuditLogChangeDto>>(`${BASE}${toParams({ ...query, page, size })}`),
  rollback: (id: string) => apiPost<AuditLogChangeDto>(`${BASE}/${id}/rollback`),
};
