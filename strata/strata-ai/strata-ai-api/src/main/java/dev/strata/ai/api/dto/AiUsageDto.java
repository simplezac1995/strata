// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.dto;

public record AiUsageDto(
        long sessionCount, long messageCount, long providerCount, long modelCount, long documentCount) {}
