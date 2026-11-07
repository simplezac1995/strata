// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import dev.strata.common.data.desensitize.Desensitize;
import dev.strata.common.data.desensitize.DesensitizeType;
import java.time.Instant;
import java.util.List;

/**
 * Account DTO. {@code mfaEnabled} mirrors {@code sys_user.totp_enabled}: whether a confirmed TOTP
 * second factor is bound. The raw secret is never exposed here.
 */
public record UserDto(
        String id,
        String username,
        String nickname,
        @Desensitize(type = DesensitizeType.EMAIL) String email,
        @Desensitize(type = DesensitizeType.PHONE) String phone,
        String avatar,
        boolean emailVerified,
        boolean enabled,
        List<String> roleIds,
        Instant createdAt,
        boolean mfaEnabled) {

    /** Backward-compatible constructor for callers that do not track profile extras / MFA. */
    public UserDto(
            String id,
            String username,
            String nickname,
            String email,
            boolean enabled,
            List<String> roleIds,
            Instant createdAt) {
        this(id, username, nickname, email, null, null, false, enabled, roleIds, createdAt, false);
    }
}
