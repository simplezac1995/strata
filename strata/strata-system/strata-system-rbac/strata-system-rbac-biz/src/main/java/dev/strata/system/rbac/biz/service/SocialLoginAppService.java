// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.system.rbac.biz.service.support.SocialLoginStateStore;
import dev.strata.system.rbac.biz.service.support.SocialLoginStateStore.Entry;
import dev.strata.system.rbac.biz.service.support.SocialLoginStateStore.Session;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Skeleton social (third-party) login flow with the security primitives made real (P2-9):
 *
 * <ul>
 *   <li>{@code state} is a one-time, server-stored nonce (anti-CSRF).
 *   <li>PKCE {@code code_verifier} is generated and stored server-side; only the S256 challenge leaves.
 *   <li>{@code redirect_uri} must exactly match a configured whitelist (no open redirect at the IdP step).
 *   <li>Game-launch / aggregator bounce URLs must be {@code https} on a configured domain whitelist.
 * </ul>
 *
 * <p>The actual provider token exchange is intentionally not wired yet (off by default); this service only
 * issues and validates the security handshake so the primitives are testable and ready to plug into a provider.
 */
@Service
public class SocialLoginAppService {

    private final SocialLoginStateStore stateStore;
    private final boolean enabled;
    private final List<String> allowedRedirectUris;
    private final List<String> allowedLaunchDomains;

    public SocialLoginAppService(
            SocialLoginStateStore stateStore,
            @Value("${strata.social.enabled:false}") boolean enabled,
            @Value("${strata.social.allowed-redirect-uris:}") List<String> allowedRedirectUris,
            @Value("${strata.social.allowed-launch-domains:}") List<String> allowedLaunchDomains) {
        this.stateStore = stateStore;
        this.enabled = enabled;
        this.allowedRedirectUris = allowedRedirectUris == null ? List.of() : allowedRedirectUris;
        this.allowedLaunchDomains = allowedLaunchDomains == null ? List.of() : allowedLaunchDomains;
    }

    /**
     * Starts a social login: enforces the {@code redirect_uri} whitelist, mints a one-time state + PKCE
     * verifier pair server-side, and returns the state to echo plus a ready-to-use provider authorize URL.
     */
    public SocialAuthorizeResponse begin(String provider, String redirectUri) {
        if (!enabled) {
            throw new BusinessException(ErrorCode.FEATURE_NOT_ENABLED, "Social login is not enabled");
        }
        requireWhitelistedRedirectUri(redirectUri);
        Session session = stateStore.issue(provider, redirectUri);
        String authorizeUrl = buildAuthorizeUrl(provider, redirectUri, session);
        return new SocialAuthorizeResponse(session.state(), authorizeUrl, session.codeChallenge());
    }

    /**
     * Consumes the one-time state returned by the provider callback. Rejects replay / expired / unknown
     * states and a provider mismatch, then hands back the stored verifier a later token exchange needs (PKCE).
     */
    public Entry consumeCallback(String provider, String state) {
        Entry entry = stateStore.consume(state);
        if (!entry.provider().equals(provider)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Social login state does not match provider");
        }
        return entry;
    }

    /**
     * Open-redirect guard (P2-9) for game-launch / aggregator bounce URLs: the URL must be {@code https} and
     * its host must be on the configured launch-domain whitelist. Returns the validated URI for the caller to
     * redirect to.
     */
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
            if (a.isEmpty()) {
                continue;
            }
            // Exact host, or a whitelisted parent domain ("example.com" also allows "*.example.com").
            if (normalized.equals(a) || normalized.endsWith("." + a)) {
                return true;
            }
        }
        return false;
    }

    private String buildAuthorizeUrl(String provider, String redirectUri, Session session) {
        // Skeleton: the real provider base URL / client id come from config once a provider is wired.
        return "https://" + provider + ".strata.example/oauth2/authorize"
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
