// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import java.util.Map;

/** HTTP boundary for the OIDC provider used by the login flow (discovery, code exchange, userinfo). */
public interface OidcProviderClient {

    /** Endpoints for one provider, discovered from its issuer or configured explicitly. */
    record ProviderMetadata(String authorizationEndpoint, String tokenEndpoint, String userInfoEndpoint) {}

    ProviderMetadata metadata(String providerId, SocialLoginProperties.Provider provider);

    /**
     * Exchanges an authorization code (with the server-held PKCE verifier) for tokens.
     *
     * @return the token endpoint response, including {@code access_token}
     */
    Map<String, Object> exchangeCode(
            String providerId,
            SocialLoginProperties.Provider provider,
            String redirectUri,
            String code,
            String codeVerifier);

    /** Calls the provider userinfo endpoint with the access token. */
    Map<String, Object> userInfo(String providerId, SocialLoginProperties.Provider provider, String accessToken);
}
