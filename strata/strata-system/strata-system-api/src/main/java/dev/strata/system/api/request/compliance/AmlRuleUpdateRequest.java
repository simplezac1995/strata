// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AmlRuleUpdateRequest(
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Pattern(regexp = "large_deposit|large_withdraw|velocity|unusual_pattern") String ruleType,
        @NotNull @PositiveOrZero Long thresholdMinor,
        @NotBlank @Pattern(regexp = "report|freeze_account") String action,
        boolean enabled) {}
