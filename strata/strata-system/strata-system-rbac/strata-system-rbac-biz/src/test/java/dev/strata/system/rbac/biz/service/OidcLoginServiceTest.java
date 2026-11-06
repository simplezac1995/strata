// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.system.api.dto.TokenResponse;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.api.entity.UserRole;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import dev.strata.system.rbac.biz.repository.UserRepository;
import dev.strata.system.rbac.biz.repository.UserRoleRepository;
import dev.strata.system.rbac.biz.service.support.OidcProviderClient;
import dev.strata.system.rbac.biz.service.support.SocialLoginProperties;
import dev.strata.system.rbac.biz.service.support.SocialLoginStateStore;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("OIDC 登录完成流程测试")
class OidcLoginServiceTest {

    @Mock
    private SocialLoginAppService socialLoginAppService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthAppService authAppService;

    @Mock
    private OidcProviderClient oidcProviderClient;

    @Mock
    private TokenResponse tokenResponse;

    private OidcLoginService service;
    private final SocialLoginStateStore.Entry entry = new SocialLoginStateStore.Entry(
            "verifier",
            "challenge",
            "keycloak",
            "https://app.example.com/sso-callback",
            Instant.now().plusSeconds(60));

    @BeforeEach
    void setUp() {
        service = new OidcLoginService(
                socialLoginAppService,
                userRepository,
                roleRepository,
                userRoleRepository,
                passwordEncoder,
                authAppService);
    }

    private SocialLoginProperties.Provider provider(boolean autoProvision, String roleCode) {
        SocialLoginProperties.Provider provider = new SocialLoginProperties.Provider();
        provider.setClientId("strata-admin");
        provider.setClientSecret("secret");
        provider.setAutoProvision(autoProvision);
        provider.setDefaultRoleCode(roleCode);
        return provider;
    }

    private void arrangeHandshake(SocialLoginProperties.Provider provider) {
        when(socialLoginAppService.consumeCallback("keycloak", "state")).thenReturn(entry);
        when(socialLoginAppService.requireProvider("keycloak")).thenReturn(provider);
        when(socialLoginAppService.oidcProviderClient()).thenReturn(oidcProviderClient);
    }

    private static User user(long id, String email, boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setUsername("alice");
        user.setEmail(email);
        user.setEnabled(enabled);
        return user;
    }

    @Test
    @DisplayName("已有启用用户：按 email 匹配并签发常规会话")
    void complete_existingUser_issuesSession() {
        SocialLoginProperties.Provider provider = provider(false, "");
        arrangeHandshake(provider);
        User user = user(7L, "alice@example.com", true);
        when(oidcProviderClient.exchangeCode("keycloak", provider, entry.redirectUri(), "code", "verifier"))
                .thenReturn(Map.of("access_token", "at"));
        when(oidcProviderClient.userInfo("keycloak", provider, "at"))
                .thenReturn(Map.of("email", "alice@example.com", "email_verified", true));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(authAppService.issueTokensForUser(eq(user), eq("127.0.0.1"), any()))
                .thenReturn(tokenResponse);

        TokenResponse result = service.complete("keycloak", "code", "state", "127.0.0.1", "curl");

        assertThat(result).isSameAs(tokenResponse);
    }

    @Test
    @DisplayName("未知邮箱且允许自动开户：创建用户并授予默认角色")
    void complete_unknownEmail_autoProvisionsWithRole() {
        SocialLoginProperties.Provider provider = provider(true, "viewer");
        arrangeHandshake(provider);
        when(oidcProviderClient.exchangeCode(anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(Map.of("access_token", "at"));
        when(oidcProviderClient.userInfo(anyString(), any(), anyString()))
                .thenReturn(Map.of(
                        "email", "new@example.com",
                        "email_verified", true,
                        "name", "New User",
                        "preferred_username", "new.user"));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameAndDeletedFalse("new.user")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("bcrypt");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(9L);
            return saved;
        });
        Role role = new Role();
        role.setId(3L);
        role.setCode("viewer");
        when(roleRepository.findByCodeAndDeletedFalse("viewer")).thenReturn(Optional.of(role));
        when(authAppService.issueTokensForUser(any(User.class), anyString(), any()))
                .thenReturn(tokenResponse);

        TokenResponse result = service.complete("keycloak", "code", "state", "127.0.0.1", "curl");

        assertThat(result).isSameAs(tokenResponse);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("new.user");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(userCaptor.getValue().isEmailVerified()).isTrue();
        ArgumentCaptor<UserRole> linkCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(linkCaptor.capture());
        assertThat(linkCaptor.getValue().getUserId()).isEqualTo(9L);
        assertThat(linkCaptor.getValue().getRoleId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("邮箱未验证：拒绝")
    void complete_unverifiedEmail_rejected() {
        SocialLoginProperties.Provider provider = provider(true, "");
        arrangeHandshake(provider);
        when(oidcProviderClient.exchangeCode(anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(Map.of("access_token", "at"));
        when(oidcProviderClient.userInfo(anyString(), any(), anyString()))
                .thenReturn(Map.of("email", "alice@example.com", "email_verified", false));

        assertThatThrownBy(() -> service.complete("keycloak", "code", "state", "ip", "ua"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("缺少 email：拒绝")
    void complete_missingEmail_rejected() {
        SocialLoginProperties.Provider provider = provider(true, "");
        arrangeHandshake(provider);
        when(oidcProviderClient.exchangeCode(anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(Map.of("access_token", "at"));
        when(oidcProviderClient.userInfo(anyString(), any(), anyString())).thenReturn(Map.of("sub", "123"));

        assertThatThrownBy(() -> service.complete("keycloak", "code", "state", "ip", "ua"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("用户被禁用：拒绝")
    void complete_disabledUser_rejected() {
        SocialLoginProperties.Provider provider = provider(false, "");
        arrangeHandshake(provider);
        when(oidcProviderClient.exchangeCode(anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(Map.of("access_token", "at"));
        when(oidcProviderClient.userInfo(anyString(), any(), anyString()))
                .thenReturn(Map.of("email", "alice@example.com", "email_verified", true));
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user(7L, "alice@example.com", false)));

        assertThatThrownBy(() -> service.complete("keycloak", "code", "state", "ip", "ua"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.ACCOUNT_DISABLED);
    }

    @Test
    @DisplayName("自动开户关闭且用户不存在：拒绝")
    void complete_unknownUserWithoutProvisioning_rejected() {
        SocialLoginProperties.Provider provider = provider(false, "");
        arrangeHandshake(provider);
        when(oidcProviderClient.exchangeCode(anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(Map.of("access_token", "at"));
        when(oidcProviderClient.userInfo(anyString(), any(), anyString()))
                .thenReturn(Map.of("email", "new@example.com", "email_verified", true));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete("keycloak", "code", "state", "ip", "ua"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.FEATURE_NOT_ENABLED);
    }

    @Test
    @DisplayName("缺少 code/state：拒绝")
    void complete_missingCode_rejected() {
        assertThatThrownBy(() -> service.complete("keycloak", "", "state", "ip", "ua"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("令牌交换没有 access_token：拒绝")
    void complete_noAccessToken_rejected() {
        SocialLoginProperties.Provider provider = provider(true, "");
        arrangeHandshake(provider);
        when(oidcProviderClient.exchangeCode(anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(Map.of());

        assertThatThrownBy(() -> service.complete("keycloak", "code", "state", "ip", "ua"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
