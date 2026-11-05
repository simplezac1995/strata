// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.dto;

import java.math.BigDecimal;
import java.util.List;

/** Aggregated billing summary over a window, aligned with the frontend {@code AiBillingSummary}. */
public record AiBillingSummary(int days, List<AiBillingDailyPoint> daily, Totals totals) {

    public record Totals(
            long callCount, long promptTokens, long completionTokens, long totalTokens, BigDecimal costMinor) {}
}
