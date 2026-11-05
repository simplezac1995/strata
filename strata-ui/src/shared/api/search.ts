// SPDX-License-Identifier: Apache-2.0
import { apiGet, toParams } from "@/shared/api/client";

/* ─── Global cross-domain search (contract-aligned, do not guess fields) ── */

export interface SearchHitDto {
  id: string;
  label: string;
  subtitle: string | null;
  route: string;
}

export interface SearchResultDto {
  users: SearchHitDto[];
  roles: SearchHitDto[];
  orders: SearchHitDto[];
  pages: SearchHitDto[];
}

export const searchApi = {
  search: (q: string, limit = 10) =>
    apiGet<SearchResultDto>(`/api/v1/system/search${toParams({ q, limit })}`),
};
