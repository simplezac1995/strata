// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto.compliance;

import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeRateDto(
        String id, String fromCurrency, String toCurrency, BigDecimal rate, Instant updatedAt, Instant createdAt) {}
