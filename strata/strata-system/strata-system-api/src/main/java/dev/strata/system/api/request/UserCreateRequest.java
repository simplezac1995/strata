// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserCreateRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(min = 6, max = 64) String password,
        @Size(max = 64) String nickname,
        boolean enabled,
        List<@Pattern(regexp = "\\d+", message = "must be numeric") String> roleIds) {}
