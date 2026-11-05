// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record AiBillingDaySummary(
        String date, long totalTokens, BigDecimal totalCost, List<AiBillingModelUsage> byModel) {}
