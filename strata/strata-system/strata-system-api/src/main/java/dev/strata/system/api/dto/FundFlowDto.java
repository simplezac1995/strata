// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

public record FundFlowDto(
        String id,
        String userId,
        String accountType,
        long changeAmountMinor,
        long balanceAfterMinor,
        String bizType,
        String bizId,
        Instant createdAt) {}
