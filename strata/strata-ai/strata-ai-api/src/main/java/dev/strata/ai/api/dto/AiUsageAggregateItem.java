// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.dto;

import java.math.BigDecimal;

public record AiUsageAggregateItem(
        String groupKey,
        long callCount,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        BigDecimal costMinor) {}
