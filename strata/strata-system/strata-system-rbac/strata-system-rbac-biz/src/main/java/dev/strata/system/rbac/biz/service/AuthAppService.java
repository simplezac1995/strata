// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.security.jwt.JwtProperties;
import dev.strata.common.security.jwt.JwtTokenService;
import dev.strata.system.api.dto.CurrentSessionResponse;
import dev.strata.system.api.dto.TokenResponse;
import dev.strata.system.api.dto.UserDto;
import dev.strata.system.api.event.LoginLogEvent;
import dev.strata.system.api.request.ChangePasswordRequest;
import dev.strata.system.api.request.ForgotPasswordRequest;
import dev.strata.system.api.request.LoginRequest;
import dev.strata.system.api.request.MfaVerifyRequest;
import dev.strata.system.api.request.RefreshTokenRequest;
import dev.strata.system.api.request.RegisterRequest;
import dev.strata.system.api.request.ResetPasswordRequest;
import dev.strata.system.api.request.VerifyEmailRequest;
import dev.strata.system.ops.api.entity.LoginLog;
import dev.strata.system.rbac.api.entity.Menu;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.api.entity.RoleMenu;
import dev.strata.system.rbac.api.entity.Tenant;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.api.entity.UserRole;
import dev.strata.system.rbac.biz.mapper.UserMapper;
import dev.strata.system.rbac.biz.metrics.RbacMetrics;
import dev.strata.system.rbac.biz.repository.MenuRepository;
import dev.strata.system.rbac.biz.repository.RoleMenuRepository;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import dev.strata.system.rbac.biz.repository.TenantRepository;
import dev.strata.system.rbac.biz.repository.UserRepository;
import dev.strata.system.rbac.biz.repository.UserRoleRepository;
import dev.strata.system.rbac.biz.security.AdminCaptchaOutcome;
import dev.strata.system.rbac.biz.security.AdminCaptchaService;
import dev.strata.system.rbac.biz.security.TotpService;
import dev.strata.system.rbac.biz.service.support.LoginLockService;
import dev.strata.system.rbac.biz.service.support.MfaChallengeStore;
import dev.strata.system.rbac.biz.service.support.TokenBlacklistService;
import dev.strata.system.rbac.biz.service.support.UserAgentParser;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthAppService {

    private static final Logger log = LoggerFactory.getLogger(AuthAppService.class);
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Fixed BCrypt hash burned for timing equalization (P1-4) when the account does not exist.
     *
     * <p>The login path must cost the same CPU whether the username is unknown or the password is simply
     * wrong: otherwise an attacker measures response time to enumerate which usernames are registered
     * (timing attack). When the lookup misses we run one {@code matches()} against this constant instead of
     * short-circuiting. It is the well-known BCrypt hash of the literal string {@code "password"}; even if a
     * caller happens to supply that plaintext, we always throw the same generic error afterwards, so it can
     * never be turned into a valid login or an oracle.
     */
    private static final String DUMMY_BCRYPT_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    /**
     * Comma-separated list of default passwords that require immediate change.
     * Users logging in with one of these passwords will be flagged with mustChangePassword=true.
     *
     * <p>Bound with SpEL so a comma-separated property actually splits into a {@code List<String>}: a bare
     * {@code @Value("")} cannot inject a scalar string into a list, so the gate below never saw the configured
     * defaults. Missing property defaults to an empty list (P2-16).
     */
    @Value("#{'${strata.security.default-passwords:}'.split(',')}")
    private List<String> defaultPasswords;

    /**
     * Gate for the [DEV-MAIL] log lines that print a password-reset token / email verification code in plaintext
     * (no real mailer yet). Off by default: in production a reset token must never reach a log file, because a log
     * read is a password reset. Turn it on only in local dev via strata.mail.dev-log-enabled=true.
     */
    @Value("${strata.mail.dev-log-enabled:false}")
    private boolean devMailLogEnabled;

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final MenuRepository menuRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final MenuAppService menuAppService;
    private final LoginLockService loginLockService;
    private final TokenBlacklistService tokenBlacklistService;
    private final ApplicationEventPublisher eventPublisher;
    private final RbacMetrics rbacMetrics;
    private final AdminCaptchaService adminCaptchaService;
    private final TotpAppService totpAppService;
    private final TotpService totpService;
    private final MfaChallengeStore mfaChallengeStore;

    public AuthAppService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            RoleMenuRepository roleMenuRepository,
            MenuRepository menuRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties,
            UserMapper userMapper,
            MenuAppService menuAppService,
            LoginLockService loginLockService,
            TokenBlacklistService tokenBlacklistService,
            ApplicationEventPublisher eventPublisher,
            RbacMetrics rbacMetrics,
            AdminCaptchaService adminCaptchaService,
            TotpAppService totpAppService,
            TotpService totpService,
            MfaChallengeStore mfaChallengeStore) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.menuRepository = menuRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.userMapper = userMapper;
        this.menuAppService = menuAppService;
        this.loginLockService = loginLockService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.eventPublisher = eventPublisher;
        this.rbacMetrics = rbacMetrics;
        this.adminCaptchaService = adminCaptchaService;
        this.totpAppService = totpAppService;
        this.totpService = totpService;
        this.mfaChallengeStore = mfaChallengeStore;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request, String clientIp, String userAgent) {
        String lockKey = request.username() + "|" + clientIp;
        /* Per-IP abuse gate: once an IP has failed enough times inside the sliding window it must clear a math
        captcha before its credentials are even checked. Below the threshold this is a no-op. */
        if (adminCaptchaService.requiresCaptcha(clientIp)) {
            AdminCaptchaOutcome outcome = adminCaptchaService.verify(request.captchaId(), request.captchaCode());
            if (outcome == AdminCaptchaOutcome.MISSING) {
                throw new BusinessException(ErrorCode.CAPTCHA_REQUIRED, "Captcha verification is required");
            }
            if (outcome == AdminCaptchaOutcome.EXPIRED) {
                throw new BusinessException(ErrorCode.CAPTCHA_EXPIRED, "Captcha has expired or was already used");
            }
            if (outcome != AdminCaptchaOutcome.OK) {
                throw new BusinessException(ErrorCode.CAPTCHA_INVALID, "Captcha answer is incorrect");
            }
        }
        try {
            loginLockService.checkNotLocked(lockKey);
        } catch (BusinessException ex) {
            log.error("Error in AuthAppService: {}", ex.getMessage(), ex);
            publishLoginLog(request.username(), clientIp, userAgent, LoginLog.STATUS_FAIL);
            rbacMetrics.recordLoginFailure();
            throw ex;
        }
        User user =
                userRepository.findByUsernameAndDeletedFalse(request.username()).orElse(null);
        if (user == null) {
            /* Timing equalization (P1-4): the account does not exist. Burn one BCrypt verification against the
            fixed dummy hash so this path costs the same as a wrong-password attempt below, instead of returning
            immediately and leaking which usernames are registered by response time. The result is discarded. */
            passwordEncoder.matches(request.password(), DUMMY_BCRYPT_HASH);
            recordFailedLogin(lockKey, clientIp, request.username(), userAgent);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid username or password");
        }
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordFailedLogin(lockKey, clientIp, request.username(), userAgent);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid username or password");
        }
        loginLockService.clear(lockKey);
        adminCaptchaService.clear(clientIp);
        // Second factor: a correct password for an MFA-enabled account must NOT issue a session on its own.
        // Hand back a short-lived challenge token; /auth/mfa/verify completes the login once the code checks out.
        if (totpAppService.isEnabled(user)) {
            String challenge = mfaChallengeStore.issue(user.getId());
            return TokenResponse.mfaRequired(challenge, jwtProperties.accessTokenTtlSeconds());
        }
        publishLoginLog(request.username(), clientIp, userAgent, LoginLog.STATUS_SUCCESS);
        rbacMetrics.recordLoginSuccess();
        // Check if user is using a default password that requires immediate change
        boolean mustChangePassword = isUsingDefaultPassword(request.password());
        if (mustChangePassword) {
            log.warn("User '{}' is using a default password and must change it", request.username());
        }
        return issueTokens(user, mustChangePassword);
    }

    /**
     * Completes an MFA-gated login: consumes the challenge token minted by {@link #login} and verifies the
     * TOTP code before issuing a session. A wrong code rejects without issuing tokens and without consuming
     * the challenge (so the admin can retry), while an unknown/expired challenge is always rejected.
     */
    @Transactional(readOnly = true)
    public TokenResponse mfaVerify(MfaVerifyRequest request, String clientIp, String userAgent) {
        Long userId = mfaChallengeStore.consume(request.mfaToken());
        User user = userRepository
                .findById(userId)
                .filter(u -> !u.isDeleted() && u.isEnabled())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "User not found"));
        String secret = totpAppService.decryptSecret(user);
        if (secret == null || !totpService.verify(secret, request.code())) {
            rbacMetrics.recordLoginFailure();
            publishLoginLog(user.getUsername(), clientIp, userAgent, LoginLog.STATUS_FAIL);
            throw new BusinessException(ErrorCode.MFA_INVALID_CODE, "Invalid verification code");
        }
        publishLoginLog(user.getUsername(), clientIp, userAgent, LoginLog.STATUS_SUCCESS);
        rbacMetrics.recordLoginSuccess();
        return issueTokens(user, false);
    }

    /** Issues a normal framework session for an externally authenticated user (SSO / OpenID Connect). */
    public TokenResponse issueTokensForUser(User user, String clientIp, String userAgent) {
        publishLoginLog(user.getUsername(), clientIp, userAgent, LoginLog.STATUS_SUCCESS);
        rbacMetrics.recordLoginSuccess();
        return issueTokens(user, false);
    }

    private void publishLoginLog(String username, String clientIp, String userAgent, String status) {
        eventPublisher.publishEvent(
                new LoginLogEvent(username, clientIp, UserAgentParser.browserOf(userAgent), status));
    }

    /**
     * Shared failure bookkeeping for every rejected credential attempt (unknown account, disabled account,
     * wrong password). Kept identical across branches so the lock/captcha/metric side-effects cannot become a
     * timing side-channel either.
     */
    private void recordFailedLogin(String lockKey, String clientIp, String username, String userAgent) {
        loginLockService.recordFailure(lockKey);
        adminCaptchaService.recordFailure(clientIp);
        publishLoginLog(username, clientIp, userAgent, LoginLog.STATUS_FAIL);
        rbacMetrics.recordLoginFailure();
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshTokenRequest request) {
        Claims claims;
        try {
            claims = jwtTokenService.parse(request.refreshToken());
        } catch (Exception ex) {
            log.error("Error in AuthAppService: {}", ex.getMessage(), ex);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid refresh token");
        }
        if (!jwtTokenService.isRefreshToken(claims)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid refresh token");
        }
        if (tokenBlacklistService.isBlacklisted(claims)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token revoked");
        }
        Long userId = Long.valueOf(claims.getSubject());
        User user = userRepository
                .findById(userId)
                .filter(u -> !u.isDeleted() && u.isEnabled())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "User not found"));
        TokenResponse tokens = issueTokens(user, false);
        if (tokenBlacklistService.isEnabled()) {
            /* Single-use rotation (ADR-0008): revoke the old refresh token once a new pair is issued. */
            tokenBlacklistService.blacklist(claims);
        }
        return tokens;
    }

    /** Server-side logout: revoke the refresh token when the blacklist is enabled (ADR-0008). */
    @Transactional(readOnly = true)
    public void logout(RefreshTokenRequest request) {
        try {
            Claims claims = jwtTokenService.parse(request.refreshToken());
            if (jwtTokenService.isRefreshToken(claims)) {
                tokenBlacklistService.blacklist(claims);
            }
        } catch (Exception ignored) {
            log.error("Error in AuthAppService: {}", ignored.getMessage(), ignored);
            // already expired or malformed 鈥?nothing to revoke
        }
    }

    /**
     * Self-service registration (dev mode): creates a tenant record plus an enabled user.
     * Row-level multi-tenancy is out of MVP scope (ADR/钀藉湴鎵ц鏂规 搂2): the user is not
     * linked to the tenant and gets no roles 鈥?an admin assigns roles afterwards.
     */
    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.username())) {
            passwordEncoder.matches(request.password(), DUMMY_BCRYPT_HASH);
            throw new BusinessException(
                    ErrorCode.CONFLICT, "Registration could not be completed; please review your details");
        }
        if (request.email() != null && !request.email().isBlank() && userRepository.existsByEmail(request.email())) {
            passwordEncoder.matches(request.password(), DUMMY_BCRYPT_HASH);
            throw new BusinessException(
                    ErrorCode.CONFLICT, "Registration could not be completed; please review your details");
        }
        Tenant tenant = new Tenant();
        tenant.setCode(request.slug());
        tenant.setName(request.workspace());
        tenant.setStatus("active");
        tenant.setPlan("free");
        tenantRepository.save(tenant);

        User user = new User();
        user.setUsername(request.username());
        user.setNickname(request.nickname());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        userRepository.save(user);
        return userMapper.toDto(user, List.of());
    }

    /** Issues a password reset token; in dev the "email" is written to the log. */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository
                .findByUsernameAndDeletedFalse(request.username().trim())
                .or(() -> userRepository.findByEmail(request.username().trim()))
                .ifPresent(user -> {
                    String token = newToken();
                    /* Store only the hash 鈥?a DB leak must not expose usable reset tokens. */
                    user.setResetToken(sha256(token));
                    user.setResetTokenExpiry(Instant.now().plus(RESET_TOKEN_TTL));
                    userRepository.save(user);
                    /* Dev-mode "email" (no mailer yet): remove once email delivery is wired. */
                    if (devMailLogEnabled) {
                        log.info("[DEV-MAIL] Password reset token for {}: {}", user.getUsername(), token);
                    }
                });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository
                .findByResetToken(sha256(request.token().trim()))
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Invalid or expired reset token"));
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid or expired reset token");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    /**
     * Self-service password change (P1-8 audited). The {@code @OperationLog} aspect records the actor and
     * outcome to the security audit log on success; the new/old passwords themselves are never logged.
     */
    @OperationLog(module = "auth:password", action = "change")
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository
                .findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /** Dev mode: accounts are auto-verified at registration; the code is only logged. */
    @Transactional(readOnly = true)
    public void verifyEmail(VerifyEmailRequest request) {
        if (devMailLogEnabled) {
            log.info("[DEV-MAIL] Email verification code submitted: {}", request.code());
        }
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            log.error("Error in AuthAppService: {}", e.getMessage(), e);
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Check if the given plaintext password matches any configured default password.
     *
     * @param plainPassword the password to check
     * @return true if the password is a default password that requires change
     */
    private boolean isUsingDefaultPassword(String plainPassword) {
        if (plainPassword == null || defaultPasswords == null || defaultPasswords.isEmpty()) {
            return false;
        }
        return defaultPasswords.stream().anyMatch(defaultPassword -> defaultPassword.equals(plainPassword));
    }

    /** Current user + menu tree for the admin console session refresh ({@code GET /auth/me}). */
    @Transactional(readOnly = true)
    public CurrentSessionResponse currentSession(Long userId) {
        User user = userRepository
                .findById(userId)
                .filter(candidate -> !candidate.isDeleted() && candidate.isEnabled())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "User not found"));
        List<Long> linkedRoleIds = userRoleRepository.findByUserId(userId).stream()
                .map(UserRole::getRoleId)
                .toList();
        List<Long> roleIds = roleRepository.findByDeletedFalseAndEnabledTrueOrderByIdAsc().stream()
                .map(Role::getId)
                .filter(linkedRoleIds::contains)
                .toList();
        List<Menu> menus = resolveMenus(roleIds);
        return new CurrentSessionResponse(
                userMapper.toDto(user, roleIds.stream().map(String::valueOf).toList()),
                menuAppService.buildTree(menus));
    }

    private TokenResponse issueTokens(User user, boolean mustChangePassword) {
        List<Long> linkedRoleIds = userRoleRepository.findByUserId(user.getId()).stream()
                .map(UserRole::getRoleId)
                .toList();
        /* Only active roles may grant authorities 鈥?deleted/disabled roles are revoked at
        the next login/refresh (see RoleAppService.delete which also cleans associations). */
        List<Long> roleIds = roleRepository.findByDeletedFalseAndEnabledTrueOrderByIdAsc().stream()
                .map(Role::getId)
                .filter(linkedRoleIds::contains)
                .toList();
        List<Menu> menus = resolveMenus(roleIds);
        List<String> authorities = menus.stream()
                .map(Menu::getPermission)
                .filter(permission -> permission != null && !permission.isBlank())
                .distinct()
                .toList();
        String access = jwtTokenService.createAccessToken(user.getId(), user.getUsername(), authorities);
        String refresh = jwtTokenService.createRefreshToken(user.getId(), user.getUsername());
        UserDto userDto =
                userMapper.toDto(user, roleIds.stream().map(String::valueOf).toList());
        return new TokenResponse(
                access,
                refresh,
                "Bearer",
                jwtProperties.accessTokenTtlSeconds(),
                userDto,
                menuAppService.buildTree(menus),
                mustChangePassword);
    }

    private List<Menu> resolveMenus(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = roleMenuRepository.findByRoleIdIn(roleIds).stream()
                .map(RoleMenu::getMenuId)
                .distinct()
                .toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        // P1-12: push the enabled filter and the id set into one indexed query instead of loading every enabled
        // menu and filtering it in memory.
        return menuRepository.findByIdInAndEnabledTrueOrderBySortOrderAscIdAsc(menuIds);
    }
}
