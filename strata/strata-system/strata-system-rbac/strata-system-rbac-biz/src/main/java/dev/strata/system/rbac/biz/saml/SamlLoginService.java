// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.saml;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.system.api.dto.TokenResponse;
import dev.strata.system.api.request.RefreshTokenRequest;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.api.entity.UserRole;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import dev.strata.system.rbac.biz.repository.UserRepository;
import dev.strata.system.rbac.biz.repository.UserRoleRepository;
import dev.strata.system.rbac.biz.saml.support.OneTimeTokenStore;
import dev.strata.system.rbac.biz.service.AuthAppService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Completes the SAML ACS: validate the assertion, map/provision the local user, issue tokens and bounce the SPA back. */
@Service
@ConditionalOnProperty(prefix = "strata.saml", name = "enabled", havingValue = "true")
public class SamlLoginService {

    private final SamlProperties properties;
    private final SamlRequestService requestService;
    private final SamlAssertionValidator validator;
    private final OneTimeTokenStore loginCodeStore;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthAppService authAppService;

    public SamlLoginService(
            SamlProperties properties,
            SamlRequestService requestService,
            SamlAssertionValidator validator,
            OneTimeTokenStore samlLoginCodeStore,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthAppService authAppService) {
        this.properties = properties;
        this.requestService = requestService;
        this.validator = validator;
        this.loginCodeStore = samlLoginCodeStore;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authAppService = authAppService;
    }

    @Transactional
    public String complete(String samlResponse, String relayState, String clientIp, String userAgent) {
        String requestId = requestService.requestIdFor(relayState);
        SamlIdentity identity = validator.validate(samlResponse, requestId);
        String email = identity.email().trim().toLowerCase();
        User user = userRepository
                .findByEmail(email)
                .filter(candidate -> !candidate.isDeleted())
                .orElse(null);
        if (user == null) {
            if (!properties.isAutoProvision()) {
                throw new BusinessException(
                        ErrorCode.FEATURE_NOT_ENABLED, "SAML user does not exist and auto-provisioning is disabled");
            }
            user = provision(email, identity);
        }
        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "User is disabled");
        }
        TokenResponse tokens = authAppService.issueTokensForUser(user, clientIp, userAgent);
        String code = loginCodeStore.newKey();
        loginCodeStore.put(code, tokens.refreshToken());
        String callback = properties.getFrontendCallbackUrl();
        return callback
                + (callback.contains("?") ? "&" : "?")
                + "provider=saml&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);
    }

    public TokenResponse exchange(String code) {
        String refreshToken = loginCodeStore.take(code);
        return authAppService.refresh(new RefreshTokenRequest(refreshToken));
    }

    private User provision(String email, SamlIdentity identity) {
        String base = (identity.displayName() == null || identity.displayName().isBlank())
                ? email.substring(0, email.indexOf('@'))
                : identity.displayName();
        String username = base.replaceAll("[^A-Za-z0-9._-]", "-");
        if (username.isBlank()) {
            username = "saml-user";
        }
        if (username.length() > 48) {
            username = username.substring(0, 48);
        }
        String candidate = username;
        int suffix = 1;
        while (userRepository.existsByUsernameAndDeletedFalse(candidate)) {
            candidate = username + "-" + suffix++;
        }
        User user = new User();
        user.setUsername(candidate);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setNickname(identity.displayName() == null ? candidate : identity.displayName());
        user.setEmail(email);
        user.setEmailVerified(true);
        user.setEnabled(true);
        User saved = userRepository.save(user);
        String roleCode = properties.getDefaultRoleCode();
        if (roleCode != null && !roleCode.isBlank()) {
            Role role = roleRepository.findByCodeAndDeletedFalse(roleCode).orElse(null);
            if (role != null) {
                UserRole link = new UserRole();
                link.setUserId(saved.getId());
                link.setRoleId(role.getId());
                userRoleRepository.save(link);
            }
        }
        return saved;
    }
}
