// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostUpdateRequest(
        @NotBlank @Size(max = 64) String code, @NotBlank @Size(max = 64) String name, int sortOrder, boolean enabled) {}
