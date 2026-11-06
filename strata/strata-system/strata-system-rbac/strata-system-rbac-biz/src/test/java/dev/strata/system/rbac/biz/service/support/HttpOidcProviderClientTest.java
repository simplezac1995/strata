// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("OIDC provider HTTP 客户端测试")
class HttpOidcProviderClientTest {

    private MockRestServiceServer server;
    private HttpOidcProviderClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpOidcProviderClient(builder);
    }

    private static SocialLoginProperties.Provider provider() {
        SocialLoginProperties.Provider provider = new SocialLoginProperties.Provider();
        provider.setClientId("strata-admin");
        provider.setClientSecret("secret");
        provider.setTokenEndpoint("https://id.example.com/token");
        provider.setUserInfoEndpoint("https://id.example.com/userinfo");
        provider.setAuthorizationEndpoint("https://id.example.com/authorize");
        return provider;
    }

    @Test
    @DisplayName("显式端点：用 PKCE/redirect_uri 换取令牌并携带 Bearer 拉取 userinfo")
    void exchangeAndUserInfo_ok() {
        SocialLoginProperties.Provider provider = provider();
        server.expect(requestTo("https://id.example.com/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"at\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://id.example.com/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer at"))
                .andRespond(withSuccess("{\"email\":\"a@b.c\"}", MediaType.APPLICATION_JSON));

        Map<String, Object> tokens =
                client.exchangeCode("keycloak", provider, "https://app.example.com/sso-callback", "code", "verifier");
        Map<String, Object> identity = client.userInfo("keycloak", provider, "at");

        assertThat(tokens).containsEntry("access_token", "at");
        assertThat(identity).containsEntry("email", "a@b.c");
        server.verify();
    }

    @Test
    @DisplayName("只给 issuer 时从 discovery 文档解析端点")
    void discovery_ok() {
        SocialLoginProperties.Provider provider = new SocialLoginProperties.Provider();
        provider.setIssuerUri("https://id.example.com/");
        server.expect(requestTo("https://id.example.com/.well-known/openid-configuration"))
                .andRespond(withSuccess(
                        "{\"authorization_endpoint\":\"https://id.example.com/auth\","
                                + "\"token_endpoint\":\"https://id.example.com/token\","
                                + "\"userinfo_endpoint\":\"https://id.example.com/userinfo\"}",
                        MediaType.APPLICATION_JSON));

        OidcProviderClient.ProviderMetadata metadata = client.metadata("keycloak", provider);

        assertThat(metadata.authorizationEndpoint()).isEqualTo("https://id.example.com/auth");
        assertThat(metadata.tokenEndpoint()).isEqualTo("https://id.example.com/token");
        assertThat(metadata.userInfoEndpoint()).isEqualTo("https://id.example.com/userinfo");
        server.verify();
    }

    @Test
    @DisplayName("令牌端点返回 400：映射为 UNAUTHORIZED")
    void exchangeError_rejected() {
        SocialLoginProperties.Provider provider = provider();
        server.expect(requestTo("https://id.example.com/token"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(
                        org.springframework.http.HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.exchangeCode("keycloak", provider, "https://app/cb", "code", "verifier"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("没有 issuer 也没有显式端点：FEATURE_NOT_ENABLED")
    void missingEndpoints_rejected() {
        SocialLoginProperties.Provider provider = new SocialLoginProperties.Provider();
        assertThatThrownBy(() -> client.metadata("keycloak", provider))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.FEATURE_NOT_ENABLED);
    }
}
