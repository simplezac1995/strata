// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Confirms an in-progress TOTP enrollment: the admin scans the QR / enters the secret into their
 * authenticator and submits the generated code. A matching code flips {@code totpEnabled} on.
 */
public record TotpConfirmRequest(
        @NotBlank @Pattern(regexp = "\\d{6}", message = "code must be 6 digits") String code) {}
