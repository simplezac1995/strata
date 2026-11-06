// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RiskRuleCreateRequest(
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Pattern(regexp = "ip_blacklist|device|amount|frequency") String type,
        @NotNull @PositiveOrZero Integer threshold,
        @NotBlank @Pattern(regexp = "reject|freeze|alert") String action,
        boolean enabled) {}
