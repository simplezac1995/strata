// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

public record RiskIpBlacklistDto(String id, String ipAddress, String reason, Instant expiresAt, Instant createdAt) {}
