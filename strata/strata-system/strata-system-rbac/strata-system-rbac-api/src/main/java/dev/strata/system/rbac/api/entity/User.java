// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(
        name = "sys_user",
        indexes = {
            @Index(name = "idx_user_email", columnList = "email"),
            @Index(name = "idx_user_tenant_id", columnList = "tenant_id"),
            @Index(name = "idx_user_enabled", columnList = "enabled"),
            @Index(name = "idx_user_deleted", columnList = "deleted"),
            @Index(name = "idx_user_tenant_enabled", columnList = "tenant_id, enabled"),
            @Index(name = "idx_user_reset_token", columnList = "reset_token")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends AuditingEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    /** BCrypt credential hash. Excluded from {@code toString()} (P1-8) so it can never leak via logs. */
    @ToString.Exclude
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(length = 64)
    private String nickname;

    @Column(length = 128)
    private String email;

    /**
     * Whether {@link #email} has been verified. A self-service email change resets this to {@code false};
     * this batch does not send a verification mail (TODO: wire the email verification flow before trusting it).
     */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    /**
     * Bound mobile number. Self-service binding this batch writes the number directly without an SMS OTP
     * (TODO: gate on an SMS verification code before production trust).
     */
    @Column(length = 32)
    private String phone;

    /** Public avatar URL, produced by the existing file upload endpoint. */
    @Column(length = 512)
    private String avatar;

    /**
     * SHA-256 of the one-time password-reset token (never the raw token). Excluded from {@code toString()}
     * (P1-8) alongside the credential material so a logged entity dump cannot leak it.
     */
    @ToString.Exclude
    @Column(name = "reset_token", length = 128)
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private Instant resetTokenExpiry;

    /**
     * Base32 TOTP secret stored AES-256-GCM encrypted at rest (see {@code FieldEncryptor}); it is never
     * returned to the client after enrollment and never logged. {@code null} until enrollment starts.
     */
    @ToString.Exclude
    @Column(name = "totp_secret", length = 512)
    private String totpSecret;

    /**
     * Gates the second login factor. Enrollment leaves this {@code false} until the admin confirms a valid
     * code; only {@code true} requires a TOTP verification step at login.
     */
    @Column(name = "totp_enabled", nullable = false)
    private boolean totpEnabled = false;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "tenant_id")
    private Long tenantId;
}
