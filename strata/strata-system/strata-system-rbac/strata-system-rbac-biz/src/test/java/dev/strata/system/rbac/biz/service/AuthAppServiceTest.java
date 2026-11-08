// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.security.jwt.JwtProperties;
import dev.strata.common.security.jwt.JwtTokenService;
import dev.strata.system.api.dto.TokenResponse;
import dev.strata.system.api.event.LoginLogEvent;
import dev.strata.system.api.request.LoginRequest;
import dev.strata.system.api.request.MfaVerifyRequest;
import dev.strata.system.api.request.RefreshTokenRequest;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.biz.mapper.UserMapper;
import dev.strata.system.rbac.biz.metrics.RbacMetrics;
import dev.strata.system.rbac.biz.repository.MenuRepository;
import dev.strata.system.rbac.biz.repository.RoleMenuRepository;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import dev.strata.system.rbac.biz.repository.TenantRepository;
import dev.strata.system.rbac.biz.repository.UserRepository;
import dev.strata.system.rbac.biz.repository.UserRoleRepository;
import dev.strata.system.rbac.biz.security.AdminCaptchaService;
import dev.strata.system.rbac.biz.security.TotpService;
import dev.strata.system.rbac.biz.service.support.LoginLockService;
import dev.strata.system.rbac.biz.service.support.MfaChallengeStore;
import dev.strata.system.rbac.biz.service.support.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMenuRepository roleMenuRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MenuAppService menuAppService;

    @Mock
    private LoginLockService loginLockService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RbacMetrics rbacMetrics;

    @Mock
    private AdminCaptchaService adminCaptchaService;

    @Mock
    private TotpAppService totpAppService;

    @Mock
    private TotpService totpService;

    @Mock
    private MfaChallengeStore mfaChallengeStore;

    @Mock
    private Claims claims;

    private AuthAppService authAppService;

    @BeforeEach
    void setUp() {
        authAppService = new AuthAppService(
                userRepository,
                userRoleRepository,
                roleRepository,
                roleMenuRepository,
                menuRepository,
                tenantRepository,
                passwordEncoder,
                jwtTokenService,
                jwtProperties,
                userMapper,
                menuAppService,
                loginLockService,
                tokenBlacklistService,
                eventPublisher,
                rbacMetrics,
                adminCaptchaService,
                totpAppService,
                totpService,
                mfaChallengeStore);
    }

    private User createTestUser(String username, boolean enabled) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPasswordHash("encoded-password-hash");
        user.setEnabled(enabled);
        user.setDeleted(false);
        return user;
    }

    @Nested
    class LoginTests {

        @Test
        void shouldReturnTokensWhenLoginSuccess() {
            LoginRequest request = new LoginRequest("admin", "password123");
            User user = createTestUser("admin", true);

            when(userRepository.findByUsernameAndDeletedFalse("admin")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "encoded-password-hash"))
                    .thenReturn(true);
            when(jwtTokenService.createAccessToken(anyLong(), anyString(), any()))
                    .thenReturn("access-token");
            when(jwtTokenService.createRefreshToken(anyLong(), anyString())).thenReturn("refresh-token");

            TokenResponse response = authAppService.login(request, "127.0.0.1", "Mozilla/5.0");

            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            verify(loginLockService).clear(anyString());
            verify(rbacMetrics).recordLoginSuccess();
        }

        @Test
        void shouldFlagMustChangePasswordWhenUsingConfiguredDefaultPassword() throws Exception {
            setDefaultPasswords(java.util.List.of("changeme"));
            LoginRequest request = new LoginRequest("admin", "changeme");
            User user = createTestUser("admin", true);
            when(userRepository.findByUsernameAndDeletedFalse("admin")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("changeme", "encoded-password-hash")).thenReturn(true);
            when(jwtTokenService.createAccessToken(anyLong(), anyString(), any()))
                    .thenReturn("access-token");
            when(jwtTokenService.createRefreshToken(anyLong(), anyString())).thenReturn("refresh-token");

            TokenResponse response = authAppService.login(request, "127.0.0.1", "Mozilla/5.0");

            assertThat(response.mustChangePassword()).isTrue();
        }

        @Test
        void shouldNotFlagMustChangePasswordWhenUsingNonDefaultPassword() throws Exception {
            setDefaultPasswords(java.util.List.of("changeme"));
            LoginRequest request = new LoginRequest("admin", "unique-pass-123");
            User user = createTestUser("admin", true);
            when(userRepository.findByUsernameAndDeletedFalse("admin")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("unique-pass-123", "encoded-password-hash"))
                    .thenReturn(true);
            when(jwtTokenService.createAccessToken(anyLong(), anyString(), any()))
                    .thenReturn("access-token");
            when(jwtTokenService.createRefreshToken(anyLong(), anyString())).thenReturn("refresh-token");

            TokenResponse response = authAppService.login(request, "127.0.0.1", "Mozilla/5.0");

            assertThat(response.mustChangePassword()).isFalse();
        }

        private void setDefaultPasswords(java.util.List<String> values) throws Exception {
            java.lang.reflect.Field field = AuthAppService.class.getDeclaredField("defaultPasswords");
            field.setAccessible(true);
            field.set(authAppService, values);
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            LoginRequest request = new LoginRequest("nonexistent", "password123");

            when(userRepository.findByUsernameAndDeletedFalse("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authAppService.login(request, "127.0.0.1", "Mozilla/5.0"))
                    .isInstanceOf(BusinessException.class);

            verify(loginLockService).recordFailure(anyString());
            verify(rbacMetrics).recordLoginFailure();
        }

        @Test
        void shouldThrowWhenPasswordMismatch() {
            LoginRequest request = new LoginRequest("admin", "wrong-password");
            User user = createTestUser("admin", true);

            when(userRepository.findByUsernameAndDeletedFalse("admin")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong-password", "encoded-password-hash"))
                    .thenReturn(false);

            assertThatThrownBy(() -> authAppService.login(request, "127.0.0.1", "Mozilla/5.0"))
                    .isInstanceOf(BusinessException.class);

            verify(loginLockService).recordFailure(anyString());
            verify(rbacMetrics).recordLoginFailure();
        }

        @Test
        void shouldThrowWhenUserDisabled() {
            LoginRequest request = new LoginRequest("disabled-user", "password123");
            User user = createTestUser("disabled-user", false);

            when(userRepository.findByUsernameAndDeletedFalse("disabled-user")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authAppService.login(request, "127.0.0.1", "Mozilla/5.0"))
                    .isInstanceOf(BusinessException.class);

            verify(loginLockService).recordFailure(anyString());
            verify(rbacMetrics).recordLoginFailure();
        }

        @Test
        void shouldThrowWhenAccountLocked() {
            LoginRequest request = new LoginRequest("locked-user", "password123");

            org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.UNAUTHORIZED, "Account locked"))
                    .when(loginLockService)
                    .checkNotLocked(anyString());

            assertThatThrownBy(() -> authAppService.login(request, "127.0.0.1", "Mozilla/5.0"))
                    .isInstanceOf(BusinessException.class);

            verify(userRepository, never()).findByUsernameAndDeletedFalse(anyString());
            verify(rbacMetrics).recordLoginFailure();
        }
    }

    @Nested
    class MfaLoginTests {

        @Test
        void loginBlocksSessionWhenMfaEnabled() {
            LoginRequest request = new LoginRequest("admin", "password123");
            User user = createTestUser("admin", true);
            user.setTotpEnabled(true);

            when(userRepository.findByUsernameAndDeletedFalse("admin")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "encoded-password-hash"))
                    .thenReturn(true);
            when(totpAppService.isEnabled(user)).thenReturn(true);
            when(mfaChallengeStore.issue(1L)).thenReturn("challenge-token");
            when(jwtProperties.accessTokenTtlSeconds()).thenReturn(3600L);

            TokenResponse response = authAppService.login(request, "127.0.0.1", "Mozilla/5.0");

            assertThat(response.mfaRequired()).isTrue();
            assertThat(response.mfaToken()).isEqualTo("challenge-token");
            assertThat(response.accessToken()).isNull();
            assertThat(response.refreshToken()).isNull();
            // No session tokens must be minted for the password-only call.
            verify(jwtTokenService, never()).createAccessToken(anyLong(), anyString(), any());
        }

        @Test
        void loginIssuesSessionWhenMfaDisabled() {
            LoginRequest request = new LoginRequest("admin", "password123");
            User user = createTestUser("admin", true);

            when(userRepository.findByUsernameAndDeletedFalse("admin")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "encoded-password-hash"))
                    .thenReturn(true);
            when(totpAppService.isEnabled(user)).thenReturn(false);
            when(jwtTokenService.createAccessToken(anyLong(), anyString(), any()))
                    .thenReturn("access");
            when(jwtTokenService.createRefreshToken(anyLong(), anyString())).thenReturn("refresh");

            TokenResponse response = authAppService.login(request, "127.0.0.1", "Mozilla/5.0");

            assertThat(response.mfaRequired()).isFalse();
            assertThat(response.accessToken()).isEqualTo("access");
        }

        @Test
        void mfaVerifyIssuesTokensOnValidCode() {
            User user = createTestUser("admin", true);
            when(mfaChallengeStore.consume("challenge-token")).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(totpAppService.decryptSecret(user)).thenReturn("plain-secret");
            when(totpService.verify("plain-secret", "123456")).thenReturn(true);
            when(jwtTokenService.createAccessToken(anyLong(), anyString(), any()))
                    .thenReturn("access");
            when(jwtTokenService.createRefreshToken(anyLong(), anyString())).thenReturn("refresh");
            when(userRoleRepository.findByUserId(1L)).thenReturn(java.util.List.of());
            when(roleRepository.findByDeletedFalseAndEnabledTrueOrderByIdAsc()).thenReturn(java.util.List.of());

            TokenResponse response =
                    authAppService.mfaVerify(new MfaVerifyRequest("challenge-token", "123456"), "127.0.0.1", "ua");

            assertThat(response.accessToken()).isEqualTo("access");
            verify(rbacMetrics).recordLoginSuccess();
        }

        @Test
        void mfaVerifyRejectsWrongCode() {
            User user = createTestUser("admin", true);
            when(mfaChallengeStore.consume("challenge-token")).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(totpAppService.decryptSecret(user)).thenReturn("plain-secret");
            when(totpService.verify("plain-secret", "999999")).thenReturn(false);

            assertThatThrownBy(() -> authAppService.mfaVerify(
                            new MfaVerifyRequest("challenge-token", "999999"), "127.0.0.1", "ua"))
                    .isInstanceOf(BusinessException.class);
            verify(rbacMetrics).recordLoginFailure();
        }

        @Test
        void mfaVerifyRejectsBadChallenge() {
            when(mfaChallengeStore.consume("junk"))
                    .thenThrow(new BusinessException(ErrorCode.MFA_CHALLENGE_EXPIRED, "bad"));

            assertThatThrownBy(
                            () -> authAppService.mfaVerify(new MfaVerifyRequest("junk", "123456"), "127.0.0.1", "ua"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    class RefreshTokenTests {

        @Test
        void shouldReturnNewTokensWhenRefreshSuccess() {
            RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
            User user = createTestUser("admin", true);

            when(jwtTokenService.parse("valid-refresh-token")).thenReturn(claims);
            when(jwtTokenService.isRefreshToken(claims)).thenReturn(true);
            when(tokenBlacklistService.isBlacklisted(claims)).thenReturn(false);
            when(claims.getSubject()).thenReturn("1");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(jwtTokenService.createAccessToken(anyLong(), anyString(), any()))
                    .thenReturn("new-access-token");
            when(jwtTokenService.createRefreshToken(anyLong(), anyString())).thenReturn("new-refresh-token");
            when(tokenBlacklistService.isEnabled()).thenReturn(false);

            TokenResponse response = authAppService.refresh(request);

            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("new-access-token");
            assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        }

        @Test
        void shouldThrowWhenRefreshTokenInvalid() {
            RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");

            when(jwtTokenService.parse("invalid-token")).thenThrow(new RuntimeException("Invalid token"));

            assertThatThrownBy(() -> authAppService.refresh(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid refresh token");
        }

        @Test
        void shouldThrowWhenTokenIsNotRefreshToken() {
            RefreshTokenRequest request = new RefreshTokenRequest("access-token");

            when(jwtTokenService.parse("access-token")).thenReturn(claims);
            when(jwtTokenService.isRefreshToken(claims)).thenReturn(false);

            assertThatThrownBy(() -> authAppService.refresh(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid refresh token");
        }

        @Test
        void shouldThrowWhenRefreshTokenRevoked() {
            RefreshTokenRequest request = new RefreshTokenRequest("revoked-token");

            when(jwtTokenService.parse("revoked-token")).thenReturn(claims);
            when(jwtTokenService.isRefreshToken(claims)).thenReturn(true);
            when(tokenBlacklistService.isBlacklisted(claims)).thenReturn(true);

            assertThatThrownBy(() -> authAppService.refresh(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Refresh token revoked");
        }

        @Test
        void shouldThrowWhenUserNotFoundForRefresh() {
            RefreshTokenRequest request = new RefreshTokenRequest("valid-token");

            when(jwtTokenService.parse("valid-token")).thenReturn(claims);
            when(jwtTokenService.isRefreshToken(claims)).thenReturn(true);
            when(tokenBlacklistService.isBlacklisted(claims)).thenReturn(false);
            when(claims.getSubject()).thenReturn("999");
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authAppService.refresh(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    class BrowserParsingTests {

        @Test
        void shouldLoginWithBlankUserAgent() {
            LoginRequest request = new LoginRequest("admin", "password123");
            User user = createTestUser("admin", true);

            when(userRepository.findByUsernameAndDeletedFalse("admin")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtTokenService.createAccessToken(anyLong(), anyString(), any()))
                    .thenReturn("token");
            when(jwtTokenService.createRefreshToken(anyLong(), anyString())).thenReturn("refresh");

            TokenResponse response = authAppService.login(request, "127.0.0.1", "");

            assertThat(response).isNotNull();
            verify(eventPublisher).publishEvent(any(LoginLogEvent.class));
        }
    }
}
