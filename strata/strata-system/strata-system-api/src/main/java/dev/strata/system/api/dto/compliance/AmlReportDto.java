// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto.compliance;

import java.time.Instant;

public record AmlReportDto(
        String id,
        String userId,
        String reportType,
        Long amountMinor,
        String description,
        String status,
        Instant createdAt) {}
