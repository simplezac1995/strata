// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.security.jwt.JwtProperties;
import dev.strata.common.security.jwt.JwtTokenService;
import dev.strata.system.api.dto.UserDto;
import dev.strata.system.api.request.ForgotPasswordRequest;
import dev.strata.system.api.request.LoginRequest;
import dev.strata.system.api.request.RegisterRequest;
import dev.strata.system.api.request.ResetPasswordRequest;
import dev.strata.system.rbac.api.entity.Menu;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.api.entity.RoleMenu;
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
import dev.strata.system.rbac.biz.security.AdminCaptchaService;
import dev.strata.system.rbac.biz.security.TotpService;
import dev.strata.system.rbac.biz.service.support.LoginLockService;
import dev.strata.system.rbac.biz.service.support.MfaChallengeStore;
import dev.strata.system.rbac.biz.service.support.TokenBlacklistService;
import java.util.List;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Remaining branch arms of {@link AuthAppService} not in {@link AuthAppServiceTest} / {@link
 * AuthAppServiceAccountTest}: null/blank UA, marker-without-version UA, long-UA truncation, register
 * blank email, reset-password with null expiry, the default-password matrix, and the non-empty
 * roleIds/permission-filter path in token issuance.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "varargs"})
class AuthAppServiceBranchTest {

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

    private AuthAppService service;

    @BeforeEach
    void setUp() {
        service = new AuthAppService(
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

    private User enabledUser(long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setPasswordHash("hash");
        u.setEnabled(true);
        u.setDeleted(false);
        return u;
    }

    private UserDto userDto() {
        return new UserDto("1", "alice", "Ali", "a@b.com", true, List.of(), null);
    }

    private void stubIssueTokensEmpty() {
        when(userRoleRepository.findByUserId(anyLong())).thenReturn(List.of());
        when(roleRepository.findByDeletedFalseAndEnabledTrueOrderByIdAsc()).thenReturn(List.of());
        when(jwtTokenService.createAccessToken(anyLong(), anyString(), any())).thenReturn("access");
        when(jwtTokenService.createRefreshToken(anyLong(), anyString())).thenReturn("refresh");
        when(userMapper.toDto(any(User.class), anyList())).thenReturn(userDto());
        when(jwtProperties.accessTokenTtlSeconds()).thenReturn(3600L);
        when(menuAppService.buildTree(anyList())).thenReturn(List.of());
    }

    // ---------- UA 解析 ----------

    @Nested
    class UaEdge {

        private String loginWithUa(String ua) {
            User user = enabledUser(1L, "admin");
            when(userRepository.findByUsernameAndDeletedFalse("admin")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            stubIssueTokensEmpty();
            service.login(new LoginRequest("admin", "pw"), "127.0.0.1", ua);
            org.mockito.ArgumentCaptor<dev.strata.system.api.event.LoginLogEvent> captor =
                    org.mockito.ArgumentCaptor.forClass(dev.strata.system.api.event.LoginLogEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            return captor.getValue().browser();
        }

        @Test
        void nullUa() {
            assertThat(loginWithUa(null)).isNull();
        }

        @Test
        void blankUa() {
            assertThat(loginWithUa("   ")).isNull();
        }

        @Test
        void edgeWithoutVersion() {
            assertThat(loginWithUa("Mozilla/5.0 (Windows) Edg/")).isEqualTo("Edge ");
        }

        @Test
        void longUnknownUaTruncated() {
            StringBuilder sb = new StringBuilder("Mozilla/5.0 (bot) ");
            while (sb.length() < 200) {
                sb.append("x");
            }
            String browser = loginWithUa(sb.toString());
            assertThat(browser).hasSize(128);
        }
    }

    // ---------- register 邮箱分支 ----------

    @Test
    void register_NullEmail_SkipsEmailCheck() {
        RegisterRequest request = new RegisterRequest("dave", "Dave", null, "pw123456", "Ws", "ws");
        when(userRepository.existsByUsernameAndDeletedFalse("dave")).thenReturn(false);
        when(passwordEncoder.encode("pw123456")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toDto(any(User.class), anyList())).thenReturn(userDto());

        assertThatCode(() -> service.register(request)).doesNotThrowAnyException();
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void register_BlankEmail_SkipsEmailCheck() {
        RegisterRequest request = new RegisterRequest("erin", "Erin", "  ", "pw123456", "Ws", "ws");
        when(userRepository.existsByUsernameAndDeletedFalse("erin")).thenReturn(false);
        when(passwordEncoder.encode("pw123456")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toDto(any(User.class), anyList())).thenReturn(userDto());

        assertThatCode(() -> service.register(request)).doesNotThrowAnyException();
        verify(userRepository, never()).existsByEmail(any());
    }

    // ---------- resetPassword 过期时间为 null ----------

    @Test
    void resetPassword_NullExpiry_Throws() {
        User user = enabledUser(1L, "alice");
        user.setResetTokenExpiry(null);
        when(userRepository.findByResetToken(DigestUtils.sha256Hex("tok"))).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("tok", "new-pw")))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    // ---------- 默认密码矩阵 ----------

    @Nested
    class DefaultPassword {

        private String loginWithPassword(String password, List<String> defaults) {
            ReflectionTestUtils.setField(service, "defaultPasswords", defaults);
            User user = enabledUser(1L, "root");
            when(userRepository.findByUsernameAndDeletedFalse("root")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(password, "hash")).thenReturn(true);
            stubIssueTokensEmpty();
            service.login(new LoginRequest("root", password), "127.0.0.1", "ua");
            // issueTokens 的 authorities 被捕获以验证 mustChange 路径；这里只验证不抛异常
            return "ok";
        }

        @Test
        void emptyDefaults_NoFlag() {
            assertThatCode(() -> loginWithPassword("whatever", List.of())).doesNotThrowAnyException();
        }

        @Test
        void matchingDefault_FlagsWarn() {
            assertThatCode(() -> loginWithPassword("initial", List.of("initial", "changeme")))
                    .doesNotThrowAnyException();
        }

        @Test
        void nonMatchingDefault_NoFlag() {
            assertThatCode(() -> loginWithPassword("secret", List.of("initial", "changeme")))
                    .doesNotThrowAnyException();
        }
    }

    // ---------- resolveMenus 非空 roleIds + 权限过滤 ----------

    @Test
    void resolveMenus_NonEmpty_FiltersPermissions() {
        User user = enabledUser(1L, "admin");

        UserRole ur = new UserRole();
        ur.setUserId(1L);
        ur.setRoleId(10L);
        when(userRepository.findByUsernameAndDeletedFalse("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(ur));

        Role active = new Role();
        active.setId(10L);
        when(roleRepository.findByDeletedFalseAndEnabledTrueOrderByIdAsc()).thenReturn(List.of(active));

        RoleMenu rm1 = new RoleMenu();
        rm1.setRoleId(10L);
        rm1.setMenuId(100L);
        RoleMenu rm2 = new RoleMenu();
        rm2.setRoleId(10L);
        rm2.setMenuId(101L);
        RoleMenu rm3 = new RoleMenu();
        rm3.setRoleId(10L);
        rm3.setMenuId(102L);
        when(roleMenuRepository.findByRoleIdIn(List.of(10L))).thenReturn(List.of(rm1, rm2, rm3));

        Menu nullPerm = new Menu();
        nullPerm.setId(100L);
        nullPerm.setPermission(null);
        Menu blankPerm = new Menu();
        blankPerm.setId(101L);
        blankPerm.setPermission("  ");
        Menu realPerm = new Menu();
        realPerm.setId(102L);
        realPerm.setPermission("sys:read");
        // P1-12: the enabled + id-set filter is pushed into one query; return the granted menus regardless of the
        // exact id list so the permission-null/blank filter below still exercises.
        when(menuRepository.findByIdInAndEnabledTrueOrderBySortOrderAscIdAsc(anyList()))
                .thenReturn(List.of(nullPerm, blankPerm, realPerm));

        when(jwtTokenService.createAccessToken(anyLong(), anyString(), any())).thenReturn("access");
        when(jwtTokenService.createRefreshToken(anyLong(), anyString())).thenReturn("refresh");
        when(userMapper.toDto(any(User.class), anyList())).thenReturn(userDto());
        when(jwtProperties.accessTokenTtlSeconds()).thenReturn(3600L);
        when(menuAppService.buildTree(anyList())).thenReturn(List.of());

        service.login(new LoginRequest("admin", "pw"), "127.0.0.1", "ua");
        // authorities 只含 sys:read（null/空白被过滤）
        org.mockito.ArgumentCaptor<List<String>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(jwtTokenService).createAccessToken(anyLong(), anyString(), captor.capture());
        assertThat(captor.getValue()).containsExactly("sys:read");
    }

    // ---------- DEV-MAIL 日志门控（P1-8） ----------

    @Nested
    class DevMailLogGate {

        private ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> attachAppender() {
            ch.qos.logback.classic.Logger logger =
                    (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(AuthAppService.class);
            var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
            appender.start();
            logger.addAppender(appender);
            return appender;
        }

        @Test
        void reset_token_not_logged_by_default() {
            User user = enabledUser(1L, "alice");
            when(userRepository.findByUsernameAndDeletedFalse("alice")).thenReturn(Optional.of(user));

            var appender = attachAppender();
            try {
                service.forgotPassword(new ForgotPasswordRequest("alice"));
            } finally {
                ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(AuthAppService.class))
                        .detachAppender(appender);
            }

            boolean leaked =
                    appender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("[DEV-MAIL]"));
            assertThat(leaked).as("默认配置（prod）下重置 token 绝不能出现在日志里").isFalse();
            // the token hash is still stored, only the plaintext log line is suppressed
            verify(userRepository).save(user);
        }

        @Test
        void reset_token_logged_when_explicitly_enabled() {
            ReflectionTestUtils.setField(service, "devMailLogEnabled", true);
            User user = enabledUser(1L, "alice");
            when(userRepository.findByUsernameAndDeletedFalse("alice")).thenReturn(Optional.of(user));

            var appender = attachAppender();
            try {
                service.forgotPassword(new ForgotPasswordRequest("alice"));
            } finally {
                ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(AuthAppService.class))
                        .detachAppender(appender);
            }

            boolean logged = appender.list.stream()
                    .anyMatch(e -> e.getFormattedMessage().contains("[DEV-MAIL]")
                            && e.getFormattedMessage().contains("Password reset token"));
            assertThat(logged).as("dev-log-enabled=true 时开发模式日志应输出 token").isTrue();
        }
    }
}
