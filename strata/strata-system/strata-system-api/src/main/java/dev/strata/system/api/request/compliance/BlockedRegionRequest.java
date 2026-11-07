// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlockedRegionRequest(
        @NotBlank @Size(max = 2) String countryCode,
        @NotBlank @Size(max = 128) String countryName,
        boolean blockRegistration,
        boolean blockLogin) {}
