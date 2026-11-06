// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Exchanges a short-lived login challenge (issued by {@code /auth/login} when MFA is required) plus a
 * current TOTP code for a real session.
 */
public record MfaVerifyRequest(
        @NotBlank @Size(max = 128) String mfaToken,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "code must be 6 digits") String code) {}
