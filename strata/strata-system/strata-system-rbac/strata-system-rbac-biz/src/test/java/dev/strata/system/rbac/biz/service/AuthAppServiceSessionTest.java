// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.system.api.event.LoginLogEvent;
import dev.strata.system.api.request.LoginRequest;
import dev.strata.system.api.request.RefreshTokenRequest;
import dev.strata.system.rbac.api.entity.User;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthAppService 登出、刷新与 UA 解析")
class AuthAppServiceSessionTest extends AuthAppServiceAccountTestBase {

    @Nested
    @DisplayName("登出 logout")
    class LogoutTests {

        @Test
        @DisplayName("刷新令牌解析成功则加入黑名单")
        void logout_ValidRefresh_Blacklists() {
            when(jwtTokenService.parse("rt")).thenReturn(claims);
            when(jwtTokenService.isRefreshToken(claims)).thenReturn(true);

            assertThatCode(() -> authAppService.logout(new RefreshTokenRequest("rt")))
                    .doesNotThrowAnyException();

            verify(tokenBlacklistService).blacklist(claims);
        }

        @Test
        @DisplayName("访问令牌不解黑")
        void logout_AccessToken_NoBlacklist() {
            when(jwtTokenService.parse("at")).thenReturn(claims);
            when(jwtTokenService.isRefreshToken(claims)).thenReturn(false);

            assertThatCode(() -> authAppService.logout(new RefreshTokenRequest("at")))
                    .doesNotThrowAnyException();

            verify(tokenBlacklistService, never()).blacklist(any());
        }

        @Test
        @DisplayName("解析失败静默吞掉不抛异常")
        void logout_Malformed_Swallows() {
            when(jwtTokenService.parse("junk")).thenThrow(new RuntimeException("bad jwt"));

            assertThatCode(() -> authAppService.logout(new RefreshTokenRequest("junk")))
                    .doesNotThrowAnyException();

            verify(tokenBlacklistService, never()).blacklist(any());
        }
    }

    @Nested
    @DisplayName("刷新令牌附加分支")
    class RefreshExtraTests {

        @Test
        @DisplayName("黑名单启用时旧刷新令牌被轮换撤销")
        void refresh_BlacklistEnabled_RevokesOld() {
            User user = enabledUser(1L, "alice");
            when(jwtTokenService.parse("rt")).thenReturn(claims);
            when(jwtTokenService.isRefreshToken(claims)).thenReturn(true);
            when(tokenBlacklistService.isBlacklisted(claims)).thenReturn(false);
            when(claims.getSubject()).thenReturn("1");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            stubIssueTokens(1L);
            when(tokenBlacklistService.isEnabled()).thenReturn(true);

            authAppService.refresh(new RefreshTokenRequest("rt"));

            verify(tokenBlacklistService).blacklist(claims);
        }

        @Test
        @DisplayName("已禁用用户刷新时抛出未授权")
        void refresh_DisabledUser_Throws() {
            User disabled = enabledUser(1L, "alice");
            disabled.setEnabled(false);
            when(jwtTokenService.parse("rt")).thenReturn(claims);
            when(jwtTokenService.isRefreshToken(claims)).thenReturn(true);
            when(tokenBlacklistService.isBlacklisted(claims)).thenReturn(false);
            when(claims.getSubject()).thenReturn("1");
            when(userRepository.findById(1L)).thenReturn(Optional.of(disabled));

            assertThatThrownBy(() -> authAppService.refresh(new RefreshTokenRequest("rt")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("User-Agent 浏览器解析")
    class UserAgentTests {

        private String loginWithUa(String ua) {
            User user = enabledUser(1L, "admin");
            when(userRepository.findByUsernameAndDeletedFalse("admin")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            stubIssueTokens(1L);

            authAppService.login(new LoginRequest("admin", "pw"), "127.0.0.1", ua);

            ArgumentCaptor<LoginLogEvent> captor = ArgumentCaptor.forClass(LoginLogEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            return captor.getValue().browser();
        }

        @Test
        @DisplayName("识别 Firefox")
        void parsesFirefox() {
            String browser = loginWithUa("Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0");
            assertThat(browser).isEqualTo("Firefox 120.0");
        }

        @Test
        @DisplayName("识别 Edge")
        void parsesEdge() {
            String browser = loginWithUa("Mozilla/5.0 (Windows NT 10.0) Edg/120.0.2210.91");
            assertThat(browser).isEqualTo("Edge 120.0.2210.91");
        }

        @Test
        @DisplayName("识别 Chrome")
        void parsesChrome() {
            String browser = loginWithUa("Mozilla/5.0 (Windows NT 10.0) Chrome/121.0.0.0 Safari/537.36");
            assertThat(browser).isEqualTo("Chrome 121.0.0.0");
        }

        @Test
        @DisplayName("识别 Safari")
        void parsesSafari() {
            String browser = loginWithUa("Mozilla/5.0 (Macintosh) Version/16.1 Safari/605.1.15");
            assertThat(browser).isEqualTo("Safari 16.1");
        }

        @Test
        @DisplayName("未知浏览器回退原始 UA")
        void unknownFallsBack() {
            String ua = "Mozilla/5.0 (compatible; bot)";
            String browser = loginWithUa(ua);
            assertThat(browser).isEqualTo(ua);
        }
    }
}
