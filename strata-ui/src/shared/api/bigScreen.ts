// SPDX-License-Identifier: Apache-2.0
import { apiGet, toParams } from "@/shared/api/client";

/* ─── Real-time ops dashboard DTOs (contract-aligned, do not guess fields) ── */

/** Real-time snapshot. All amounts are minor units (cents). */
export interface RealtimeDashboardDto {
  onlineCount: number;
  todayRechargeAmountMinor: number;
  todayRechargeCount: number;
  todayWithdrawAmountMinor: number;
  todayWithdrawCount: number;
  activeUsersToday: number;
}

/** One day of the game-flow trend. Amounts are minor units. */
export interface GameFlowPointDto {
  date: string;
  gameBetAmountMinor: number;
  gamePayoutAmountMinor: number;
  gamePlayerCount: number;
}

const BASE = "/api/v1/dashboard";

export const bigScreenApi = {
  realtime: () => apiGet<RealtimeDashboardDto>(`${BASE}/realtime`),
  gameFlow: (days = 7) => apiGet<GameFlowPointDto[]>(`${BASE}/game-flow${toParams({ days })}`),
};
