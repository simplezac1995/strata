// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

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
import dev.strata.system.rbac.biz.service.support.SocialLoginStateStore.Entry;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Completes a social / OpenID Connect login after the provider redirected back to the admin UI.
 *
 * <p>The browser only carries {@code code} + {@code state}; the PKCE verifier, redirect URI and client secret stay on
 * the server. Identity is read from the provider's userinfo endpoint with the exchanged access token, mapped to a local
 * user by verified email (optionally auto-provisioned with a configured default role), and a normal framework session
 * is issued so the rest of the platform stays JWT/RBAC based.
 */
@Service
public class OidcLoginService {

    private final SocialLoginAppService socialLoginAppService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthAppService authAppService;

    public OidcLoginService(
            SocialLoginAppService socialLoginAppService,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            AuthAppService authAppService) {
        this.socialLoginAppService = socialLoginAppService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authAppService = authAppService;
    }

    @Transactional
    public TokenResponse complete(String providerId, String code, String state, String clientIp, String userAgent) {
        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OIDC callback is missing code or state");
        }
        Entry entry = socialLoginAppService.consumeCallback(providerId, state);
        SocialLoginProperties.Provider provider = socialLoginAppService.requireProvider(providerId);
        OidcProviderClient client = socialLoginAppService.oidcProviderClient();
        if (client == null
                || provider.getClientId() == null
                || provider.getClientId().isBlank()) {
            throw new BusinessException(ErrorCode.FEATURE_NOT_ENABLED, "Social login provider is not configured");
        }

        Map<String, Object> tokenResponse =
                client.exchangeCode(providerId, provider, entry.redirectUri(), code, entry.codeVerifier());
        String accessToken = text(tokenResponse, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "SSO token exchange returned no access token");
        }
        Map<String, Object> identity = client.userInfo(providerId, provider, accessToken);

        String email = text(identity, "email");
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "SSO identity has no email address");
        }
        email = email.trim().toLowerCase(Locale.ROOT);
        boolean emailVerified = bool(identity.get("email_verified"));
        if (provider.isRequireVerifiedEmail() && !emailVerified) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "SSO identity email is not verified");
        }

        User user = userRepository
                .findByEmail(email)
                .filter(candidate -> !candidate.isDeleted())
                .orElse(null);
        if (user == null) {
            if (!provider.isAutoProvision()) {
                throw new BusinessException(
                        ErrorCode.FEATURE_NOT_ENABLED, "SSO user does not exist and auto-provisioning is disabled");
            }
            user = provision(email, identity, provider);
        }
        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "User is disabled");
        }
        return authAppService.issueTokensForUser(user, clientIp, userAgent);
    }

    private User provision(String email, Map<String, Object> identity, SocialLoginProperties.Provider provider) {
        String username = text(identity, "preferred_username");
        if (username == null || username.isBlank()) {
            username = email.substring(0, email.indexOf('@'));
        }
        username = username.replaceAll("[^A-Za-z0-9._-]", "-");
        if (username.isBlank()) {
            username = "sso-user";
        }
        if (username.length() > 48) {
            username = username.substring(0, 48);
        }
        String base = username;
        int suffix = 1;
        while (userRepository.existsByUsernameAndDeletedFalse(username)) {
            username = base + "-" + suffix++;
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setNickname(firstNonBlank(text(identity, "name"), username));
        user.setEmail(email);
        user.setEmailVerified(true);
        user.setEnabled(true);
        User saved = userRepository.save(user);

        String roleCode = provider.getDefaultRoleCode();
        if (roleCode != null && !roleCode.isBlank()) {
            roleRepository.findByCodeAndDeletedFalse(roleCode).ifPresent(role -> linkRole(saved, role));
        }
        return saved;
    }

    private void linkRole(User user, Role role) {
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleRepository.save(userRole);
    }

    private static String text(Map<String, Object> claims, String key) {
        Object value = claims == null ? null : claims.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static boolean bool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
