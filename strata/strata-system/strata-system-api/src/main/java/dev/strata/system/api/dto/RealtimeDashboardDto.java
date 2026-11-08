// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

/**
 * Real-time operations dashboard snapshot.
 *
 * <p>All amounts are in minor units. The casino wallet/player aggregates are not wired in this batch because
 * ops-biz must not depend on casino-api; values default to zero until a cross-module read model exists.
 */
public record RealtimeDashboardDto(
        long onlineCount,
        long todayRechargeAmountMinor,
        long todayRechargeCount,
        long todayWithdrawAmountMinor,
        long todayWithdrawCount,
        long activeUsersToday) {}
