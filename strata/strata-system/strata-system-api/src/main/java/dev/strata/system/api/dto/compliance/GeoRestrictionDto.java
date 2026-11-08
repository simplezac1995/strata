// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto.compliance;

import java.time.Instant;

public record GeoRestrictionDto(
        String id,
        String ipRangeStart,
        String ipRangeEnd,
        String countryCode,
        String action,
        String reason,
        boolean enabled,
        Instant createdAt) {}
