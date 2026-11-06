// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

/** Today's recharge / withdraw / fee roll-up for the reconciliation center header. */
public record ReconciliationSummaryDto(
        long todayRechargeAmountMinor,
        long todayRechargeCount,
        long todayWithdrawAmountMinor,
        long todayWithdrawCount,
        long todayFeeMinor) {}
