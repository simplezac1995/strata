// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.dto;

import java.math.BigDecimal;

/** One daily bucket in the billing window, aligned with the frontend {@code AiBillingDailyPoint}. */
public record AiBillingDailyPoint(
        String date,
        long callCount,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        BigDecimal costMinor) {}
