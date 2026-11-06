// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto.compliance;

import java.time.Instant;

public record ResponsibleGamblingLimitDto(
        String id,
        String userId,
        String limitType,
        Long amountMinor,
        String period,
        Instant startDate,
        Instant endDate,
        Instant createdAt) {}
