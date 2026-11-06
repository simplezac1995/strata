// SPDX-License-Identifier: Apache-2.0
import { apiDelete, apiGet, apiPost, apiPut } from "@/shared/api/client";
import type { PageResult } from "@/shared/types";

/** Business object a linkage rule can trigger on. */
export type AiLinkageObject = "user" | "order" | "recharge" | "withdraw";

/** Action the AI takes when a rule fires. */
export type AiLinkageAction = "notify" | "tag" | "email";

export interface AiLinkageRuleDto {
  id: string;
  name: string;
  object: AiLinkageObject;
  event: string;
  action: AiLinkageAction;
  enabled: boolean;
  description?: string | null;
}

export interface AiLinkageRulePayload {
  name: string;
  object: AiLinkageObject;
  event: string;
  action: AiLinkageAction;
  enabled: boolean;
  description: string | null;
}

export const aiLinkageApi = {
  page: (page: number, size: number) =>
    apiGet<PageResult<AiLinkageRuleDto>>(`/api/v1/ai/linkage/rules/page?page=${page}&size=${size}`),
  list: () => apiGet<AiLinkageRuleDto[]>("/api/v1/ai/linkage/rules"),
  create: (payload: AiLinkageRulePayload) => apiPost<void>("/api/v1/ai/linkage/rules", payload),
  update: (id: string, payload: AiLinkageRulePayload) =>
    apiPut<void>(`/api/v1/ai/linkage/rules/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/ai/linkage/rules/${id}`),
  setEnabled: (id: string, enabled: boolean) =>
    apiPut<void>(`/api/v1/ai/linkage/rules/${id}/enabled`, { enabled }),
  test: (id: string) =>
    apiPost<{ ok: boolean; message?: string }>(`/api/v1/ai/linkage/rules/${id}/test`, {}),
};
