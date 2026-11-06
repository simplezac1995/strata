// SPDX-License-Identifier: Apache-2.0
import { apiGet, toParams } from "@/shared/api/client";
import type { AiUsage } from "@/shared/api/aiOffice";

/** One daily aggregation bucket from GET /api/v1/ai/billing/summary. */
export interface AiBillingDailyPoint extends AiUsage {
  date: string;
  callCount: number;
}

/** Aggregated billing summary over a window. */
export interface AiBillingSummary {
  days: number;
  daily: AiBillingDailyPoint[];
  totals: AiUsage & { callCount: number };
}

/** One row from GET /api/v1/ai/usage/aggregate (grouped by model / action / user). */
export interface AiUsageAggregateRow extends AiUsage {
  groupKey: string;
  callCount: number;
}

export type AiUsageGroupBy = "model" | "action" | "user" | "day";

export const aiBillingApi = {
  summary: (days: number) =>
    apiGet<AiBillingSummary>(`/api/v1/ai/billing/summary${toParams({ days })}`),
  aggregate: (days: number, groupBy: AiUsageGroupBy) =>
    apiGet<AiUsageAggregateRow[]>(`/api/v1/ai/usage/aggregate${toParams({ days, groupBy })}`),
};
