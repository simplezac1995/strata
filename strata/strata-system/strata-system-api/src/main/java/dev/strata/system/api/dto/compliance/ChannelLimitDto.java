// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto.compliance;

import java.math.BigDecimal;
import java.time.Instant;

public record ChannelLimitDto(
        String id,
        String channel,
        String direction,
        Long minAmountMinor,
        Long maxAmountMinor,
        Long dailyMaxMinor,
        BigDecimal feePercent,
        boolean enabled,
        Instant createdAt) {}
