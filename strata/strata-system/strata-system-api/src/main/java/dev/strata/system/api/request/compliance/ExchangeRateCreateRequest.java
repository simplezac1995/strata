// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ExchangeRateCreateRequest(
        @NotBlank @Size(max = 8) String fromCurrency,
        @NotBlank @Size(max = 8) String toCurrency,
        @NotNull @Positive BigDecimal rate) {}
