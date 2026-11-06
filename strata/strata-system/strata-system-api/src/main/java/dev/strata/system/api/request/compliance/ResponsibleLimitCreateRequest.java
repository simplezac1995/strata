// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record ResponsibleLimitCreateRequest(
        @NotBlank @Pattern(regexp = "deposit|bet|loss") String limitType,
        @NotNull @Positive Long amountMinor,
        @NotBlank @Pattern(regexp = "daily|weekly|monthly") String period) {}
