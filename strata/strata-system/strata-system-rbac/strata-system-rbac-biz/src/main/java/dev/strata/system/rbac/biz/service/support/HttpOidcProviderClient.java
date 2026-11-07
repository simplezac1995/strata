// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Default {@link OidcProviderClient}: OpenID Connect discovery plus authorization-code exchange over HTTP. */
@Component
public class HttpOidcProviderClient implements OidcProviderClient {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final Map<String, ProviderMetadata> metadataCache = new ConcurrentHashMap<>();

    @Autowired
    public HttpOidcProviderClient(ObjectProvider<RestClient.Builder> restClientBuilder) {
        this(restClientBuilder.getIfAvailable(RestClient::builder));
    }

    /** Test-friendly constructor: builds the client from an explicit builder. */
    public HttpOidcProviderClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public ProviderMetadata metadata(String providerId, SocialLoginProperties.Provider provider) {
        return metadataCache.computeIfAbsent(providerId, key -> loadMetadata(provider));
    }

    private ProviderMetadata loadMetadata(SocialLoginProperties.Provider provider) {
        if (provider.getAuthorizationEndpoint() != null
                && provider.getTokenEndpoint() != null
                && provider.getUserInfoEndpoint() != null) {
            return new ProviderMetadata(
                    provider.getAuthorizationEndpoint(), provider.getTokenEndpoint(), provider.getUserInfoEndpoint());
        }
        String issuer = provider.getIssuerUri();
        if (issuer == null || issuer.isBlank()) {
            throw new BusinessException(ErrorCode.FEATURE_NOT_ENABLED, "SSO provider has no issuer-uri");
        }
        try {
            Map<String, Object> document = restClient
                    .get()
                    .uri(issuer.replaceAll("/$", "") + "/.well-known/openid-configuration")
                    .retrieve()
                    .body(JSON_MAP);
            if (document == null) {
                throw new BusinessException(ErrorCode.FEATURE_NOT_ENABLED, "SSO discovery document is empty");
            }
            String authorization = String.valueOf(document.get("authorization_endpoint"));
            String token = String.valueOf(document.get("token_endpoint"));
            String userInfo = String.valueOf(document.get("userinfo_endpoint"));
            return new ProviderMetadata(
                    firstNonBlank(provider.getAuthorizationEndpoint(), authorization),
                    firstNonBlank(provider.getTokenEndpoint(), token),
                    firstNonBlank(provider.getUserInfoEndpoint(), userInfo));
        } catch (RestClientResponseException e) {
            throw new BusinessException(ErrorCode.FEATURE_NOT_ENABLED, "SSO provider discovery failed");
        } catch (RuntimeException e) {
            if (e instanceof BusinessException business) {
                throw business;
            }
            throw new BusinessException(ErrorCode.FEATURE_NOT_ENABLED, "SSO provider discovery failed");
        }
    }

    @Override
    public Map<String, Object> exchangeCode(
            String providerId,
            SocialLoginProperties.Provider provider,
            String redirectUri,
            String code,
            String codeVerifier) {
        ProviderMetadata metadata = metadata(providerId, provider);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("client_id", provider.getClientId());
        if (provider.getClientSecret() != null) {
            form.add("client_secret", provider.getClientSecret());
        }
        form.add("code_verifier", codeVerifier);
        try {
            Map<String, Object> response = restClient
                    .post()
                    .uri(metadata.tokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JSON_MAP);
            if (response == null || response.get("access_token") == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "SSO token exchange returned no access token");
            }
            return response;
        } catch (RestClientResponseException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "SSO token exchange failed");
        }
    }

    @Override
    public Map<String, Object> userInfo(
            String providerId, SocialLoginProperties.Provider provider, String accessToken) {
        ProviderMetadata metadata = metadata(providerId, provider);
        try {
            Map<String, Object> response = restClient
                    .get()
                    .uri(metadata.userInfoEndpoint())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JSON_MAP);
            if (response == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "SSO userinfo response is empty");
            }
            return response;
        } catch (RestClientResponseException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "SSO userinfo request failed");
        }
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
