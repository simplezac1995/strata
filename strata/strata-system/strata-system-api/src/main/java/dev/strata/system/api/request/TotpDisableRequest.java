// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Self-service MFA disable: the admin must prove possession of an authenticator by submitting a current
 * code before the second factor is removed.
 */
public record TotpDisableRequest(
        @NotBlank @Pattern(regexp = "\\d{6}", message = "code must be 6 digits") String code) {}
