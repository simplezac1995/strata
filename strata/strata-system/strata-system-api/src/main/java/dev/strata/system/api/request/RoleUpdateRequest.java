// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RoleUpdateRequest(
        @Size(max = 64) String name,
        Boolean enabled,
        List<@Pattern(regexp = "\\d+", message = "must be numeric") String> menuIds) {}
