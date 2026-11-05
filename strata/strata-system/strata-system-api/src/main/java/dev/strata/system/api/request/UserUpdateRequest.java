// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserUpdateRequest(
        @Size(max = 64) String nickname,
        Boolean enabled,
        @Size(min = 6, max = 64) String password,
        List<@Pattern(regexp = "\\d+", message = "must be numeric") String> roleIds) {}
