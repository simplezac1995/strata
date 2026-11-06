// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto.compliance;

import java.time.Instant;

public record CoolOffDto(String id, String userId, Instant endDate, Instant createdAt) {}
