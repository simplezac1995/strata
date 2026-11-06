// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OauthClientUpdateRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 128) String clientSecret,
        @Size(max = 256) String scopes,
        Boolean enabled) {}
