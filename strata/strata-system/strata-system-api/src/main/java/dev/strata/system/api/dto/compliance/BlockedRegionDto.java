// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto.compliance;

import java.time.Instant;

public record BlockedRegionDto(
        String id,
        String countryCode,
        String countryName,
        boolean blockRegistration,
        boolean blockLogin,
        Instant createdAt) {}
