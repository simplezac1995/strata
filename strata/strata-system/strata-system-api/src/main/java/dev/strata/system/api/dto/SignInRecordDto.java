// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SignInRecordDto(
        String id,
        String userId,
        LocalDate signDate,
        int dayNumber,
        long rewardPoints,
        BigDecimal rewardMinor,
        Instant createdAt) {}
