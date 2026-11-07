import { apiDelete, apiGet, apiPost, apiPut } from "@/shared/api/client";
import type { PageResult, {{E}}Dto } from "@/shared/types";

export interface {{E}}Payload {
{{TS_PAYLOAD_FIELDS}}}

export interface {{E}}PageQuery {
{{TS_QUERY_FIELDS}}}

export const {{EL}}Api = {
  page: (page: number, size: number, query: {{E}}PageQuery = {}) => {
    const params = new URLSearchParams();
    params.set("page", String(page));
    params.set("size", String(size));
{{API_QUERY_PARAMS}}
    return apiGet<PageResult<{{E}}Dto>>(`/api/v1/{{MODULE}}/{{RESOURCE}}?${params.toString()}`);
  },
  create: (payload: {{E}}Payload) => apiPost<void>("/api/v1/{{MODULE}}/{{RESOURCE}}", payload),
  update: (id: string, payload: {{E}}Payload) =>
    apiPut<void>(`/api/v1/{{MODULE}}/{{RESOURCE}}/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/{{MODULE}}/{{RESOURCE}}/${id}`),
};
