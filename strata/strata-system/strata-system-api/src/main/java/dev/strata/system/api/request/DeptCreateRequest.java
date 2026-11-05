// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeptCreateRequest(
        @Pattern(regexp = "^\\d*$", message = "must be numeric") String parentId,
        @NotBlank @Size(max = 64) String name,
        int sortOrder,
        boolean enabled) {}
