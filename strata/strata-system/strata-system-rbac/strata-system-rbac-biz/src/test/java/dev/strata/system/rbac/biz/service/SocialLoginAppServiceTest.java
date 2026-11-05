// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.system.rbac.biz.service.support.SocialLoginStateStore;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * P2-9 social-login security primitives: redirect_uri whitelist, one-time state + PKCE, and the
 * https + domain-whitelist game-launch open-redirect guard.
 */
@DisplayName("P2-9 社交登录安全原语测试")
class SocialLoginAppServiceTest {

    private SocialLoginAppService service(List<String> redirectUris, List<String> launchDomains) {
        return new SocialLoginAppService(new SocialLoginStateStore(), true, redirectUris, launchDomains);
    }

    // ---------- redirect_uri whitelist ----------

    @Test
    @DisplayName("白名单内 redirect_uri 放行并返回 state + PKCE challenge")
    void begin_whitelistedRedirect_ok() {
        SocialLoginAppService svc = service(List.of("https://app.example.com/callback"), List.of("games.example.com"));

        SocialAuthorizeResponse resp = svc.begin("google", "https://app.example.com/callback");

        assertThat(resp.state()).isNotBlank();
        assertThat(resp.codeChallenge()).isNotBlank();
        assertThat(resp.authorizeUrl()).contains("state=" + resp.state());
        assertThat(resp.authorizeUrl()).contains("code_challenge_method=S256");
        assertThat(resp.authorizeUrl()).contains("redirect_uri=");
    }

    @Test
    @DisplayName("非白名单 redirect_uri 抛 BAD_REQUEST")
    void begin_nonWhitelistedRedirect_rejected() {
        SocialLoginAppService svc = service(List.of("https://app.example.com/callback"), List.of());
        assertThatThrownBy(() -> svc.begin("google", "https://evil.example.com/callback"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("空 redirect_uri 抛 BAD_REQUEST")
    void begin_blankRedirect_rejected() {
        SocialLoginAppService svc = service(List.of("https://app.example.com/callback"), List.of());
        assertThatThrownBy(() -> svc.begin("google", "  ")).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("未启用时 begin 抛 FEATURE_NOT_ENABLED")
    void begin_disabled_rejected() {
        SocialLoginAppService svc = new SocialLoginAppService(
                new SocialLoginStateStore(), false, List.of("https://app.example.com/callback"), List.of());
        assertThatThrownBy(() -> svc.begin("google", "https://app.example.com/callback"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.FEATURE_NOT_ENABLED);
    }

    // ---------- one-time state + PKCE ----------

    @Test
    @DisplayName("回调消费 state 一次性, 二次回放拒绝")
    void callback_stateReplay_rejected() {
        SocialLoginAppService svc = service(List.of("https://app.example.com/callback"), List.of());
        SocialAuthorizeResponse resp = svc.begin("google", "https://app.example.com/callback");

        var entry = svc.consumeCallback("google", resp.state());
        assertThat(entry.codeVerifier()).isNotBlank();

        assertThatThrownBy(() -> svc.consumeCallback("google", resp.state())).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("provider 不匹配时拒绝 state")
    void callback_providerMismatch_rejected() {
        SocialLoginAppService svc = service(List.of("https://app.example.com/callback"), List.of());
        SocialAuthorizeResponse resp = svc.begin("google", "https://app.example.com/callback");

        assertThatThrownBy(() -> svc.consumeCallback("github", resp.state())).isInstanceOf(BusinessException.class);
    }

    // ---------- game launch URL open-redirect guard ----------

    @Test
    @DisplayName("https + 白名单域名的游戏启动 URL 放行")
    void gameLaunch_httpsWhitelisted_ok() {
        SocialLoginAppService svc = service(List.of(), List.of("games.example.com"));
        URI uri = svc.requireSafeGameLaunchUrl("https://games.example.com/play?game=1");
        assertThat(uri.getHost()).isEqualTo("games.example.com");
    }

    @Test
    @DisplayName("白名单父域的子域名也放行")
    void gameLaunch_subdomainOfWhitelisted_ok() {
        SocialLoginAppService svc = service(List.of(), List.of("example.com"));
        URI uri = svc.requireSafeGameLaunchUrl("https://slot.games.example.com/play");
        assertThat(uri.getHost()).isEqualTo("slot.games.example.com");
    }

    @Test
    @DisplayName("http 协议拒绝")
    void gameLaunch_http_rejected() {
        SocialLoginAppService svc = service(List.of(), List.of("games.example.com"));
        assertThatThrownBy(() -> svc.requireSafeGameLaunchUrl("http://games.example.com/play"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("非白名单域名拒绝(防 open redirect)")
    void gameLaunch_nonWhitelistedHost_rejected() {
        SocialLoginAppService svc = service(List.of(), List.of("games.example.com"));
        assertThatThrownBy(() -> svc.requireSafeGameLaunchUrl("https://evil.com/play"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("空/非法 URL 拒绝")
    void gameLaunch_malformed_rejected() {
        SocialLoginAppService svc = service(List.of(), List.of("games.example.com"));
        assertThatThrownBy(() -> svc.requireSafeGameLaunchUrl("")).isInstanceOf(BusinessException.class);
    }
}
