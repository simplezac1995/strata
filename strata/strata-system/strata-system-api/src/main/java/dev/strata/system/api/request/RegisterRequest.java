// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Self-service registration (creates a tenant workspace + the admin user). */
public record RegisterRequest(
        @NotBlank @Size(max = 64) String username,
        @Size(max = 64) String nickname,
        @Email @Size(max = 128) String email,
        @NotBlank @Size(min = 6, max = 64) String password,
        @NotBlank @Size(max = 64) String workspace,
        @NotBlank @Size(max = 64) String slug) {}
