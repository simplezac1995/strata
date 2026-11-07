// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ReconciliationDailyDto(
        String id,
        LocalDate bizDate,
        long totalRechargeMinor,
        long totalWithdrawMinor,
        long totalFeeMinor,
        long playerBalanceMinor,
        String status,
        String diffDetail,
        Instant createdAt) {}
