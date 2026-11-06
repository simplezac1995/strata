// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Self-service change of the current user's email. */
public record UpdateMyEmailRequest(@NotBlank @Email @Size(max = 128) String email) {}
