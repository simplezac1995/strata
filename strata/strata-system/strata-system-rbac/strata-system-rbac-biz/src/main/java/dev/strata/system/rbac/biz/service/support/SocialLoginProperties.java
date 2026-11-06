// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the social / OIDC login flow ({@code strata.social.*}).
 *
 * <p>Disabled by default. Each provider entry describes one OpenID Connect provider; the auth-code + PKCE exchange
 * happens server-side, so the client secret never reaches the browser. Endpoints can be given explicitly or are
 * discovered from {@code issuer-uri} ({@code /.well-known/openid-configuration}).
 */
@ConfigurationProperties(prefix = "strata.social")
public class SocialLoginProperties {

    /** Master switch for the social login endpoints. */
    private boolean enabled = false;

    /** Exact redirect URIs the browser may use (open-redirect guard at the IdP step). */
    private List<String> allowedRedirectUris = List.of();

    /** Domains allowed for the optional game-launch bounce URL (legacy helper). */
    private List<String> allowedLaunchDomains = List.of();

    /** Configured providers, keyed by the id the admin UI uses. */
    private Map<String, Provider> providers = Map.of();

    /** Where the one-time state + PKCE verifier pair is stored: {@code memory} (default) or {@code redis}. */
    private String stateStore = "memory";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getAllowedRedirectUris() {
        return allowedRedirectUris == null ? List.of() : allowedRedirectUris;
    }

    public void setAllowedRedirectUris(List<String> allowedRedirectUris) {
        this.allowedRedirectUris = allowedRedirectUris;
    }

    public List<String> getAllowedLaunchDomains() {
        return allowedLaunchDomains == null ? List.of() : allowedLaunchDomains;
    }

    public void setAllowedLaunchDomains(List<String> allowedLaunchDomains) {
        this.allowedLaunchDomains = allowedLaunchDomains;
    }

    public Map<String, Provider> getProviders() {
        return providers == null ? Map.of() : providers;
    }

    public void setProviders(Map<String, Provider> providers) {
        this.providers = providers;
    }

    public String getStateStore() {
        return stateStore == null ? "memory" : stateStore;
    }

    public void setStateStore(String stateStore) {
        this.stateStore = stateStore;
    }

    /** One OpenID Connect provider. */
    public static class Provider {
        private String displayName = "SSO";
        private String issuerUri;
        private String authorizationEndpoint;
        private String tokenEndpoint;
        private String userInfoEndpoint;
        private String clientId;
        private String clientSecret;
        private List<String> scopes = List.of("openid", "email", "profile");
        /** Role granted to auto-provisioned users; empty means no role (an admin assigns one). */
        private String defaultRoleCode = "";
        /** Whether an unknown, verified email creates a local user on first login. */
        private boolean autoProvision = true;
        /** Reject identities whose email is not marked verified by the provider. */
        private boolean requireVerifiedEmail = true;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getIssuerUri() {
            return issuerUri;
        }

        public void setIssuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
        }

        public String getAuthorizationEndpoint() {
            return authorizationEndpoint;
        }

        public void setAuthorizationEndpoint(String authorizationEndpoint) {
            this.authorizationEndpoint = authorizationEndpoint;
        }

        public String getTokenEndpoint() {
            return tokenEndpoint;
        }

        public void setTokenEndpoint(String tokenEndpoint) {
            this.tokenEndpoint = tokenEndpoint;
        }

        public String getUserInfoEndpoint() {
            return userInfoEndpoint;
        }

        public void setUserInfoEndpoint(String userInfoEndpoint) {
            this.userInfoEndpoint = userInfoEndpoint;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public List<String> getScopes() {
            return scopes == null || scopes.isEmpty() ? List.of("openid", "email", "profile") : scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }

        public String getDefaultRoleCode() {
            return defaultRoleCode == null ? "" : defaultRoleCode;
        }

        public void setDefaultRoleCode(String defaultRoleCode) {
            this.defaultRoleCode = defaultRoleCode;
        }

        public boolean isAutoProvision() {
            return autoProvision;
        }

        public void setAutoProvision(boolean autoProvision) {
            this.autoProvision = autoProvision;
        }

        public boolean isRequireVerifiedEmail() {
            return requireVerifiedEmail;
        }

        public void setRequireVerifiedEmail(boolean requireVerifiedEmail) {
            this.requireVerifiedEmail = requireVerifiedEmail;
        }
    }
}
