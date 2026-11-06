// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ChannelLimitCreateRequest(
        @NotBlank @Pattern(regexp = "tron|bsc") String channel,
        @NotBlank @Pattern(regexp = "deposit|withdraw") String direction,
        @NotNull @PositiveOrZero Long minAmountMinor,
        @NotNull @PositiveOrZero Long maxAmountMinor,
        @NotNull @PositiveOrZero Long dailyMaxMinor,
        BigDecimal feePercent,
        boolean enabled) {}
