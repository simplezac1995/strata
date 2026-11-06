// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WebhookUpdateRequest(
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Size(max = 256) String url,
        @Size(max = 128) String secret,
        Boolean enabled) {}
