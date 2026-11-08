// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

/**
 * Result of starting a TOTP enrollment.
 *
 * @param secret        the Base32 shared secret (also shown as a manual entry key)
 * @param otpAuthUri    the {@code otpauth://totp/...} URI an authenticator app consumes
 * @param qrCodeDataUrl a {@code data:image/png;base64,...} QR code rendering of the URI, ready for an
 *     {@code <img src>}
 * @param mfaEnabled    whether MFA is already active for this account
 */
public record TotpEnrollResponse(String secret, String otpAuthUri, String qrCodeDataUrl, boolean mfaEnabled) {}
