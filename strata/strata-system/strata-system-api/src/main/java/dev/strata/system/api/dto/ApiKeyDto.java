// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

public record ApiKeyDto(
        String id,
        String name,
        String keyPrefix,
        String scopes,
        boolean enabled,
        Instant lastUsedAt,
        Instant createdAt) {}
