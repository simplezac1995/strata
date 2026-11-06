// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.security;

/**
 * The result of checking a submitted admin captcha answer.
 *
 * <p>It is an enum rather than an exception so the service stays a pure verifier and the caller (the login
 * orchestration) maps each verdict onto the right {@code ErrorCode}.
 */
public enum AdminCaptchaOutcome {
    /** The answer matched an unexpired, unused captcha; the caller may proceed. */
    OK,
    /** The captcha id or answer was missing entirely (only meaningful when a captcha is required). */
    MISSING,
    /** The id existed and was unexpired, but the answer did not match. */
    INVALID,
    /** The id was unknown, already consumed, or past its TTL. Collapsed on purpose: no oracle. */
    EXPIRED
}
