// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin login credentials. {@code captchaId}/{@code captchaCode} are optional: the server demands a captcha only
 * after the per-IP failure threshold is crossed, so they are {@code null} on the happy path and {@code @NotBlank}
 * is enforced server-side rather than via bean validation.
 */
public record LoginRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(max = 64) String password,
        @Size(max = 64) String captchaId,
        @Size(max = 16) String captchaCode) {

    /**
     * Backwards-compatible constructor for callers that do not send a captcha (the happy path below the abuse
     * threshold).
     */
    public LoginRequest(String username, String password) {
        this(username, password, null, null);
    }
}
