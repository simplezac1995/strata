// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Asserts the P1-6 cookie rewriting: every Set-Cookie gains Secure + HttpOnly + SameSite=Lax and the __Host- prefix;
 * existing attributes are not duplicated; and the filter is inert when hardening is off.
 */
@DisplayName("CookieHardeningFilter 生产 Cookie 硬化")
class CookieHardeningFilterTest {

    private CookieHardeningFilter filter(boolean hardened) {
        return new CookieHardeningFilter(
                new CookieSecurityProperties(hardened, "Lax", true, 5, Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("Set-Cookie 被改写为 Secure/HttpOnly/SameSite=Lax/__Host- 前缀")
    void setCookieIsHardened() throws Exception {
        CookieHardeningFilter f = filter(true);
        String rewritten = f.rewrite("session=abc123; Path=/; Max-Age=3600");

        assertThat(rewritten).startsWith("__Host-session=abc123");
        assertThat(rewritten).contains("Secure");
        assertThat(rewritten).contains("HttpOnly");
        assertThat(rewritten).contains("SameSite=Lax");
        assertThat(rewritten).contains("Path=/");
    }

    @Test
    @DisplayName("已有 Secure/HttpOnly/SameSite 不重复添加")
    void existingAttributesNotDuplicated() throws Exception {
        CookieHardeningFilter f = filter(true);
        String rewritten = f.rewrite("sid=v; Secure; HttpOnly; SameSite=Strict");

        assertThat(rewritten).startsWith("__Host-sid=v");
        assertThat(rewritten).contains("SameSite=Strict");
        // exactly one each
        assertThat(rewritten.split("Secure").length - 1).isEqualTo(1);
        assertThat(rewritten.split("HttpOnly").length - 1).isEqualTo(1);
    }

    @Test
    @DisplayName("已有 __Host- 前缀不再重复加")
    void alreadyPrefixedNotDoubled() throws Exception {
        CookieHardeningFilter f = filter(true);
        String rewritten = f.rewrite("__Host-sid=v");

        assertThat(rewritten).doesNotContain("__Host-__Host-");
        assertThat(rewritten).startsWith("__Host-sid=v");
    }

    @Test
    @DisplayName("关闭硬化时过滤器原样放行，不包装响应")
    void disabledLeavesResponseUnwrapped() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/x");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter(false).doFilter(req, res, chain);

        // The raw response (not a hardened wrapper) flows through.
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("Properties：idleTimeout 非法回退 0（不过期），maxConcurrentSessions<0 回退 0")
    void propertiesDefaults() {
        CookieSecurityProperties p = new CookieSecurityProperties(true, null, true, -1, Duration.ofSeconds(-1));
        assertThat(p.sameSite()).isEqualTo("Lax");
        assertThat(p.maxConcurrentSessions()).isZero();
        assertThat(p.idleTimeout()).isZero();
    }
}
