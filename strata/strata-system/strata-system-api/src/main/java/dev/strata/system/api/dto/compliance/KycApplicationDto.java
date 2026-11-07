// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto.compliance;

import java.time.Instant;

public record KycApplicationDto(
        String id,
        String userId,
        String fullName,
        String idType,
        String idNumberHash,
        String frontImageUrl,
        String backImageUrl,
        String selfieUrl,
        String status,
        String reviewerId,
        String rejectReason,
        Instant submittedAt,
        Instant reviewedAt,
        Instant createdAt) {}
