// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.security.crypto.FieldEncryptor;
import dev.strata.system.api.dto.TotpEnrollResponse;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.biz.repository.UserRepository;
import dev.strata.system.rbac.biz.security.QrCodeGenerator;
import dev.strata.system.rbac.biz.security.TotpService;
import dev.strata.system.rbac.biz.service.support.MfaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin self-service TOTP enrollment and the second-factor bookkeeping behind login.
 *
 * <p>The raw Base32 secret is only ever held in memory: it is AES-256-GCM encrypted via {@link
 * FieldEncryptor} before it touches {@code sys_user.totp_secret} and decrypted again only to verify a code.
 * It is never returned after enrollment, never logged, and excluded from {@code toString()} on the entity.
 */
@Service
public class TotpAppService {

    private static final Logger log = LoggerFactory.getLogger(TotpAppService.class);

    private final UserRepository userRepository;
    private final TotpService totpService;
    private final QrCodeGenerator qrCodeGenerator;
    private final FieldEncryptor fieldEncryptor;
    private final String issuer;

    public TotpAppService(
            UserRepository userRepository,
            TotpService totpService,
            QrCodeGenerator qrCodeGenerator,
            FieldEncryptor fieldEncryptor,
            MfaProperties mfaProperties) {
        this.userRepository = userRepository;
        this.totpService = totpService;
        this.qrCodeGenerator = qrCodeGenerator;
        this.fieldEncryptor = fieldEncryptor;
        this.issuer = mfaProperties.getIssuer();
    }

    /**
     * Starts enrollment for the current admin: generates a fresh secret, stores it encrypted and pending
     * ({@code totpEnabled} stays false), and returns the secret plus QR material. Re-enrolling while already
     * pending replaces the old pending secret; a completed enrollment must be disabled first.
     */
    @Transactional
    public TotpEnrollResponse enroll(Long userId) {
        User user = requireUser(userId);
        if (user.isTotpEnabled()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "MFA is already enabled; disable it first to re-enroll");
        }
        String secret = totpService.generateSecret();
        user.setTotpSecret(fieldEncryptor.encrypt(secret));
        userRepository.save(user);
        String uri = totpService.buildOtpAuthUri(issuer, user.getUsername(), secret);
        String qr = qrCodeGenerator.toDataUrl(uri);
        log.info("TOTP enrollment started for user id {} (mfa still pending confirmation)", userId);
        return new TotpEnrollResponse(secret, uri, qr, false);
    }

    /**
     * Confirms a pending enrollment: a valid current code flips {@code totpEnabled} on (P1-8 audited as an
     * MFA-enabling security state change; the code itself is never logged).
     */
    @OperationLog(module = "auth:mfa", action = "enable")
    @Transactional
    public void confirmEnrollment(Long userId, String code) {
        User user = requireUser(userId);
        String secret = requirePendingSecret(user);
        if (!totpService.verify(secret, code)) {
            throw new BusinessException(ErrorCode.MFA_INVALID_CODE, "Verification code does not match");
        }
        user.setTotpEnabled(true);
        userRepository.save(user);
        log.info("TOTP enrollment confirmed for user id {}", userId);
    }

    /**
     * Disables the current admin's own MFA after proving possession with a current code (P1-8 audited as an
     * MFA-disabling security state change).
     */
    @OperationLog(module = "auth:mfa", action = "disable")
    @Transactional
    public void disable(Long userId, String code) {
        User user = requireUser(userId);
        if (!user.isTotpEnabled()) {
            throw new BusinessException(ErrorCode.MFA_NOT_ENROLLED, "MFA is not enabled for this account");
        }
        String secret = fieldEncryptor.decrypt(user.getTotpSecret());
        if (!totpService.verify(secret, code)) {
            throw new BusinessException(ErrorCode.MFA_INVALID_CODE, "Verification code does not match");
        }
        clear(user);
        userRepository.save(user);
        log.info("TOTP disabled for user id {}", userId);
    }

    /**
     * Admin reset of another account's MFA (guarded by permission upstream): wipes the secret and disables the
     * second factor without requiring that user's code. Used to recover a locked-out account. P1-8 audited as
     * a privileged security state change.
     */
    @OperationLog(module = "auth:mfa", action = "reset")
    @Transactional
    public void reset(Long userId) {
        User user = requireUser(userId);
        clear(user);
        userRepository.save(user);
        log.info("TOTP reset by administrator for user id {}", userId);
    }

    /** True when the account has a confirmed, active second factor. */
    public boolean isEnabled(User user) {
        return user != null && user.isTotpEnabled();
    }

    /** Decrypts the stored secret for the login-time verification step. */
    public String decryptSecret(User user) {
        return fieldEncryptor.decrypt(user.getTotpSecret());
    }

    private String requirePendingSecret(User user) {
        if (user.getTotpSecret() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "No pending TOTP enrollment; start enrollment first");
        }
        return fieldEncryptor.decrypt(user.getTotpSecret());
    }

    private void clear(User user) {
        user.setTotpSecret(null);
        user.setTotpEnabled(false);
    }

    private User requireUser(Long userId) {
        return userRepository
                .findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found"));
    }
}
