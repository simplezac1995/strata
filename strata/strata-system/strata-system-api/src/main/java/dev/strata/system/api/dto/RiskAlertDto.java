// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

public record RiskAlertDto(
        String id,
        String userId,
        String ipAddress,
        String deviceHash,
        String ruleName,
        String detail,
        String status,
        Instant createdAt) {}
