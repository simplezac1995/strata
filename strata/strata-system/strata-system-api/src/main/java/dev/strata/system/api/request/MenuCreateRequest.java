// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MenuCreateRequest(
        @Pattern(regexp = "^\\d*$", message = "must be numeric") String parentId,
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Size(max = 32) String type,
        @Size(max = 128) String path,
        @Size(max = 128) String component,
        @Size(max = 128) String permission,
        int sortOrder,
        boolean visible,
        boolean enabled) {}
