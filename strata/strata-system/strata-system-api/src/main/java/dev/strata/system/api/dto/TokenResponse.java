// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.util.List;

/**
 * Login token response.
 *
 * <p>When the account has TOTP MFA enabled, {@code /auth/login} does NOT issue a session: it returns
 * {@code mfaRequired=true} with a short-lived {@code mfaToken} and null tokens. The client calls
 * {@code /auth/mfa/verify} with that token plus a current TOTP code to receive a populated response.
 *
 * @param accessToken     JWT access token (null when mfaRequired)
 * @param refreshToken    Refresh token for obtaining new access tokens (null when mfaRequired)
 * @param tokenType       Token type (always "Bearer")
 * @param expiresIn       Access token expiration in seconds
 * @param user            Authenticated user details
 * @param menus           User's accessible menu tree
 * @param mustChangePassword Whether the user must change their password before continuing
 * @param mfaRequired     Whether a second TOTP factor is required before a session is issued
 * @param mfaToken        Opaque single-use challenge token, present only when mfaRequired is true
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserDto user,
        List<MenuDto> menus,
        boolean mustChangePassword,
        boolean mfaRequired,
        String mfaToken) {

    /**
     * Backwards-compatible constructor without the MFA fields (mfaRequired defaults to false).
     */
    public TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserDto user,
            List<MenuDto> menus,
            boolean mustChangePassword) {
        this(accessToken, refreshToken, tokenType, expiresIn, user, menus, mustChangePassword, false, null);
    }

    /**
     * Backwards-compatible constructor without mustChangePassword or MFA fields.
     * Defaults mustChangePassword to false.
     */
    public TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserDto user,
            List<MenuDto> menus) {
        this(accessToken, refreshToken, tokenType, expiresIn, user, menus, false);
    }

    /** Builds the "second factor required" response: no session, only a challenge token. */
    public static TokenResponse mfaRequired(String mfaToken, long expiresIn) {
        return new TokenResponse(null, null, "Bearer", expiresIn, null, List.of(), false, true, mfaToken);
    }
}
