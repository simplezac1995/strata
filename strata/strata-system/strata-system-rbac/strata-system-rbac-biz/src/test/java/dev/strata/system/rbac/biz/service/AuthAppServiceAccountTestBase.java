// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import dev.strata.common.security.jwt.JwtProperties;
import dev.strata.common.security.jwt.JwtTokenService;
import dev.strata.system.api.dto.UserDto;
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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * {@link AuthAppService} 账号/会话单测共享夹具：Mock 依赖、服务实例与用户/令牌构造器。
 * 用例按主题拆到子类（注册与密码 / 登出、刷新与 UA 解析）。
 */
abstract class AuthAppServiceAccountTestBase {

    @Mock
    protected UserRepository userRepository;

    @Mock
    protected UserRoleRepository userRoleRepository;

    @Mock
    protected RoleRepository roleRepository;

    @Mock
    protected RoleMenuRepository roleMenuRepository;

    @Mock
    protected MenuRepository menuRepository;

    @Mock
    protected TenantRepository tenantRepository;

    @Mock
    protected PasswordEncoder passwordEncoder;

    @Mock
    protected JwtTokenService jwtTokenService;

    @Mock
    protected JwtProperties jwtProperties;

    @Mock
    protected UserMapper userMapper;

    @Mock
    protected MenuAppService menuAppService;

    @Mock
    protected LoginLockService loginLockService;

    @Mock
    protected TokenBlacklistService tokenBlacklistService;

    @Mock
    protected ApplicationEventPublisher eventPublisher;

    @Mock
    protected RbacMetrics rbacMetrics;

    @Mock
    protected AdminCaptchaService adminCaptchaService;

    @Mock
    protected TotpAppService totpAppService;

    @Mock
    protected TotpService totpService;

    @Mock
    protected MfaChallengeStore mfaChallengeStore;

    @Mock
    protected Claims claims;

    protected AuthAppService authAppService;

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

    protected User enabledUser(long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setPasswordHash("hash");
        u.setEnabled(true);
        u.setDeleted(false);
        return u;
    }

    protected UserDto userDto() {
        return new UserDto("1", "alice", "Ali", "a@b.com", true, List.of(), null);
    }

    /** Stubs the read-only token issuance chain so a successful login/refresh resolves. */
    protected void stubIssueTokens(long userId) {
        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of());
        when(roleRepository.findByDeletedFalseAndEnabledTrueOrderByIdAsc()).thenReturn(List.of());
        when(jwtTokenService.createAccessToken(anyLong(), anyString(), any())).thenReturn("access");
        when(jwtTokenService.createRefreshToken(anyLong(), anyString())).thenReturn("refresh");
        when(userMapper.toDto(any(User.class), anyList())).thenReturn(userDto());
        when(jwtProperties.accessTokenTtlSeconds()).thenReturn(3600L);
        when(menuAppService.buildTree(anyList())).thenReturn(List.of());
    }
}
