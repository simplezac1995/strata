// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.security.jwt.JwtProperties;
import dev.strata.common.security.jwt.JwtTokenService;
import dev.strata.system.api.request.LoginRequest;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * P1-4 login timing equalization: an unknown username must still pay one BCrypt verification so response
 * time cannot be used to enumerate which accounts exist.
 */
class AuthTimingEqualizationTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final RoleMenuRepository roleMenuRepository = mock(RoleMenuRepository.class);
    private final MenuRepository menuRepository = mock(MenuRepository.class);
    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private final JwtProperties jwtProperties = mock(JwtProperties.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final MenuAppService menuAppService = mock(MenuAppService.class);
    private final LoginLockService loginLockService = mock(LoginLockService.class);
    private final TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final RbacMetrics rbacMetrics = mock(RbacMetrics.class);
    private final AdminCaptchaService adminCaptchaService = mock(AdminCaptchaService.class);
    private final TotpAppService totpAppService = mock(TotpAppService.class);
    private final TotpService totpService = mock(TotpService.class);
    private final MfaChallengeStore mfaChallengeStore = mock(MfaChallengeStore.class);

    private final AuthAppService service = new AuthAppService(
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

    @Test
    void runsDummyBcryptWhenUserMissing() {
        when(userRepository.findByUsernameAndDeletedFalse("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ghost", "pw"), "127.0.0.1", "ua"))
                .isInstanceOf(BusinessException.class);

        // The dummy match must have run: second argument is the fixed BCrypt-format dummy hash.
        verify(passwordEncoder)
                .matches(
                        org.mockito.ArgumentMatchers.eq("pw"),
                        org.mockito.ArgumentMatchers.argThat(h -> h != null && h.startsWith("$2")));
    }

    @Test
    void realUserWrongPasswordStillCallsMatches() {
        dev.strata.system.rbac.api.entity.User user = new dev.strata.system.rbac.api.entity.User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPasswordHash("real-hash");
        user.setEnabled(true);
        user.setDeleted(false);
        when(userRepository.findByUsernameAndDeletedFalse("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw", "real-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "pw"), "127.0.0.1", "ua"))
                .isInstanceOf(BusinessException.class);

        verify(passwordEncoder).matches(anyString(), anyString());
    }
}
