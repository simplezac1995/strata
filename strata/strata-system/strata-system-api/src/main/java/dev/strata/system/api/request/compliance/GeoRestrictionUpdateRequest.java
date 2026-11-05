// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GeoRestrictionUpdateRequest(
        @NotBlank @Size(max = 64) String ipRangeStart,
        @NotBlank @Size(max = 64) String ipRangeEnd,
        @Size(max = 2) String countryCode,
        @NotBlank @Pattern(regexp = "block|warn") String action,
        @Size(max = 512) String reason,
        boolean enabled) {}
