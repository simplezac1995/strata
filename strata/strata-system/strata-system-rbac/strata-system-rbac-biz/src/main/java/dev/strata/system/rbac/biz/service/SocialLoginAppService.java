// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.system.rbac.biz.service.support.OidcProviderClient;
import dev.strata.system.rbac.biz.service.support.SocialLoginProperties;
import dev.strata.system.rbac.biz.service.support.SocialLoginStateStore;
import dev.strata.system.rbac.biz.service.support.SocialLoginStateStore.Entry;
import dev.strata.system.rbac.biz.service.support.SocialLoginStateStore.Session;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Social / OpenID Connect login handshake.
 *
 * <p>The security primitives are real: one-time server-side {@code state}, server-held PKCE verifier (S256),
 * redirect-URI whitelist and the https + domain-whitelist game-launch guard. When a provider is configured under
 * {@code strata.social.providers.*}, {@link #begin} returns that provider's authorize URL; the code exchange and
 * user provisioning live in {@link OidcLoginService}.
 */
@Service
public class SocialLoginAppService {

    private final SocialLoginStateStore stateStore;
    private final boolean enabled;
    private final List<String> allowedRedirectUris;
    private final List<String> allowedLaunchDomains;
    private final Map<String, SocialLoginProperties.Provider> providers;
    private final OidcProviderClient oidcProviderClient;

    /** Legacy (handshake-only) constructor used by unit tests; no provider endpoints configured. */
    public SocialLoginAppService(
            SocialLoginStateStore stateStore,
            boolean enabled,
            List<String> allowedRedirectUris,
            List<String> allowedLaunchDomains) {
        this(stateStore, enabled, allowedRedirectUris, allowedLaunchDomains, Map.of(), null);
    }

    @Autowired
    public SocialLoginAppService(
            SocialLoginStateStore stateStore, SocialLoginProperties properties, OidcProviderClient providerClient) {
        this(
                stateStore,
                properties.isEnabled(),
                properties.getAllowedRedirectUris(),
                properties.getAllowedLaunchDomains(),
                properties.getProviders(),
                providerClient);
    }

    private SocialLoginAppService(
            SocialLoginStateStore stateStore,
            boolean enabled,
            List<String> allowedRedirectUris,
            List<String> allowedLaunchDomains,
            Map<String, SocialLoginProperties.Provider> providers,
            OidcProviderClient oidcProviderClient) {
        this.stateStore = stateStore;
        this.enabled = enabled;
        this.allowedRedirectUris = allowedRedirectUris == null ? List.of() : allowedRedirectUris;
        this.allowedLaunchDomains = allowedLaunchDomains == null ? List.of() : allowedLaunchDomains;
        this.providers = providers == null ? Map.of() : providers;
        this.oidcProviderClient = oidcProviderClient;
    }

    /** Starts a login: validates the redirect URI, mints state + PKCE, and builds the provider authorize URL. */
    public SocialAuthorizeResponse begin(String provider, String redirectUri) {
        if (!enabled) {
            throw new BusinessException(ErrorCode.FEATURE_NOT_ENABLED, "Social login is not enabled");
        }
        requireWhitelistedRedirectUri(redirectUri);
        Session session = stateStore.issue(provider, redirectUri);
        return new SocialAuthorizeResponse(
                session.state(), buildAuthorizeUrl(provider, redirectUri, session), session.codeChallenge());
    }

    /** Resolves a configured provider, or fails with {@code FEATURE_NOT_ENABLED} when it is unknown. */
    public SocialLoginProperties.Provider requireProvider(String providerId) {
        SocialLoginProperties.Provider provider = providers.get(providerId);
        if (provider == null) {
            throw new BusinessException(ErrorCode.FEATURE_NOT_ENABLED, "Social login provider is not configured");
        }
        return provider;
    }

    public Map<String, SocialLoginProperties.Provider> providers() {
        return providers;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public OidcProviderClient oidcProviderClient() {
        return oidcProviderClient;
    }

    public Entry consumeCallback(String provider, String state) {
        Entry entry = stateStore.consume(state);
        if (!entry.provider().equals(provider)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Social login state does not match provider");
        }
        return entry;
    }

    public URI requireSafeGameLaunchUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Game launch URL is required");
        }
        URI uri;
        try {
            uri = URI.create(rawUrl);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Game launch URL is malformed");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Game launch URL must use https");
        }
        String host = uri.getHost();
        if (host == null || !isAllowedLaunchHost(host)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Game launch URL host is not in the allowed whitelist");
        }
        return uri;
    }

    private void requireWhitelistedRedirectUri(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "redirect_uri is required");
        }
        if (allowedRedirectUris.stream().noneMatch(allowed -> allowed.equals(redirectUri))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "redirect_uri is not in the allowed whitelist");
        }
    }

    private boolean isAllowedLaunchHost(String host) {
        String normalized = host.toLowerCase();
        for (String allowed : allowedLaunchDomains) {
            String a = allowed == null ? "" : allowed.trim().toLowerCase();
            if (a.isEmpty()) continue;
            if (normalized.equals(a) || normalized.endsWith("." + a)) return true;
        }
        return false;
    }

    private String buildAuthorizeUrl(String providerId, String redirectUri, Session session) {
        SocialLoginProperties.Provider provider = providers.get(providerId);
        if (provider != null
                && oidcProviderClient != null
                && provider.getClientId() != null
                && !provider.getClientId().isBlank()) {
            String authorizationEndpoint =
                    oidcProviderClient.metadata(providerId, provider).authorizationEndpoint();
            if (authorizationEndpoint != null && !authorizationEndpoint.isBlank()) {
                return authorizationEndpoint
                        + "?response_type=code"
                        + "&client_id=" + encode(provider.getClientId())
                        + "&redirect_uri=" + encode(redirectUri)
                        + "&scope=" + encode(String.join(" ", provider.getScopes()))
                        + "&state=" + encode(session.state())
                        + "&code_challenge=" + encode(session.codeChallenge())
                        + "&code_challenge_method=S256";
            }
        }
        // No provider configured: keep the legacy placeholder so the handshake stays testable.
        return "https://" + providerId + ".strata.example/oauth2/authorize"
                + "?response_type=code"
                + "&redirect_uri=" + encode(redirectUri)
                + "&state=" + session.state()
                + "&code_challenge=" + session.codeChallenge()
                + "&code_challenge_method=S256";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
