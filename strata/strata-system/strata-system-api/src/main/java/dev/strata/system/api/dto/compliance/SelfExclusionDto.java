// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto.compliance;

import java.time.Instant;

public record SelfExclusionDto(
        String id, String userId, Instant startDate, Instant endDate, String reason, Instant createdAt) {}
