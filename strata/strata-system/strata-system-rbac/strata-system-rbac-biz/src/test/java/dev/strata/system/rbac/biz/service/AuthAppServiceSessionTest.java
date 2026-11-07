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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthAppServiceSessionTest extends AuthAppServiceAccountTestBase {

    @Nested
    class LogoutTests {

        @Test
        void logout_ValidRefresh_Blacklists() {
            when(jwtTokenService.parse("rt")).thenReturn(claims);
            when(jwtTokenService.isRefreshToken(claims)).thenReturn(true);

            assertThatCode(() -> authAppService.logout(new RefreshTokenRequest("rt")))
                    .doesNotThrowAnyException();

            verify(tokenBlacklistService).blacklist(claims);
        }

        @Test
        void logout_AccessToken_NoBlacklist() {
            when(jwtTokenService.parse("at")).thenReturn(claims);
            when(jwtTokenService.isRefreshToken(claims)).thenReturn(false);

            assertThatCode(() -> authAppService.logout(new RefreshTokenRequest("at")))
                    .doesNotThrowAnyException();

            verify(tokenBlacklistService, never()).blacklist(any());
        }

        @Test
        void logout_Malformed_Swallows() {
            when(jwtTokenService.parse("junk")).thenThrow(new RuntimeException("bad jwt"));

            assertThatCode(() -> authAppService.logout(new RefreshTokenRequest("junk")))
                    .doesNotThrowAnyException();

            verify(tokenBlacklistService, never()).blacklist(any());
        }
    }

    @Nested
    class RefreshExtraTests {

        @Test
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
        void parsesFirefox() {
            String browser = loginWithUa("Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0");
            assertThat(browser).isEqualTo("Firefox 120.0");
        }

        @Test
        void parsesEdge() {
            String browser = loginWithUa("Mozilla/5.0 (Windows NT 10.0) Edg/120.0.2210.91");
            assertThat(browser).isEqualTo("Edge 120.0.2210.91");
        }

        @Test
        void parsesChrome() {
            String browser = loginWithUa("Mozilla/5.0 (Windows NT 10.0) Chrome/121.0.0.0 Safari/537.36");
            assertThat(browser).isEqualTo("Chrome 121.0.0.0");
        }

        @Test
        void parsesSafari() {
            String browser = loginWithUa("Mozilla/5.0 (Macintosh) Version/16.1 Safari/605.1.15");
            assertThat(browser).isEqualTo("Safari 16.1");
        }

        @Test
        void unknownFallsBack() {
            String ua = "Mozilla/5.0 (compatible; bot)";
            String browser = loginWithUa(ua);
            assertThat(browser).isEqualTo(ua);
        }
    }
}
