// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MenuUpdateRequest(
        @Pattern(regexp = "^\\d*$", message = "must be numeric") String parentId,
        @Size(max = 64) String name,
        @Size(max = 32) String type,
        @Size(max = 128) String path,
        @Size(max = 128) String component,
        @Size(max = 128) String permission,
        Integer sortOrder,
        Boolean visible,
        Boolean enabled) {}
