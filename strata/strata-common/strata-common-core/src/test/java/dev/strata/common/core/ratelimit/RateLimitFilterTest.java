// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Branch coverage for {@link RateLimitFilter} (enable/compiled-empty/no-match/allowed/denied, method
 * and path matching, context-path stripping, client address) and the defensive defaults in {@link
 * RateLimitProperties} and its nested {@link RateLimitProperties.Rule}.
 */
@DisplayName("RateLimitFilter 与配置默认值")
class RateLimitFilterTest {

    private static RateLimitProperties props(boolean enabled, List<RateLimitProperties.Rule> rules) {
        return new RateLimitProperties(enabled, true, rules, Duration.ofMinutes(10), 10_000);
    }

    private static RateLimitFilter filter(RateLimitProperties p, TokenBucketRateLimiter limiter) {
        return new RateLimitFilter(p, limiter, (req, res, name, retry, msg) -> {
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(retry));
        });
    }

    private static MockHttpServletRequest get(String method, String uri, String contextPath) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        req.setContextPath(contextPath);
        req.setRemoteAddr("203.0.113.7");
        return req;
    }

    @Test
    @DisplayName("过滤器关闭时直接放行")
    void disabledFilterPassesThrough() throws Exception {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(Clock.systemUTC(), 100);
        RateLimitFilter f = filter(props(false, List.of()), limiter);
        MockHttpServletRequest req = get("POST", "/v1/auth/login", "");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        f.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("无规则时直接放行")
    void emptyRulesPassesThrough() throws Exception {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(Clock.systemUTC(), 100);
        RateLimitFilter f = filter(props(true, List.of()), limiter);
        MockHttpServletRequest req = get("POST", "/anything", "");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        f.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("路径不匹配规则时放行")
    void noMatchPassesThrough() throws Exception {
        RateLimitProperties.Rule rule =
                RateLimitProperties.Rule.of("login", "/v1/auth/**", List.of("POST"), 5, Duration.ofMinutes(1));
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(Clock.systemUTC(), 100);
        RateLimitFilter f = filter(props(true, List.of(rule)), limiter);
        MockHttpServletRequest req = get("POST", "/v1/other", "");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        f.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("方法不在规则内时跳过该规则")
    void methodNotInRuleSkips() throws Exception {
        RateLimitProperties.Rule rule =
                RateLimitProperties.Rule.of("login", "/v1/auth/**", List.of("POST"), 5, Duration.ofMinutes(1));
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(Clock.systemUTC(), 100);
        RateLimitFilter f = filter(props(true, List.of(rule)), limiter);
        MockHttpServletRequest req = get("GET", "/v1/auth/login", "");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        f.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("匹配且令牌充足：放行并写入限流头")
    void matchedAllowedSetsHeadersAndContinues() throws Exception {
        RateLimitProperties.Rule rule =
                RateLimitProperties.Rule.of("login", "/v1/auth/**", List.of("POST"), 10, Duration.ofMinutes(1));
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(Clock.systemUTC(), 100);
        RateLimitFilter f = filter(props(true, List.of(rule)), limiter);
        MockHttpServletRequest req = get("POST", "/v1/auth/login", "");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        f.doFilter(req, res, chain);

        assertThat(res.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(res.getHeader("X-RateLimit-Remaining")).isEqualTo("9");
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("匹配且令牌耗尽：写 429 且不继续链路")
    void matchedDeniedWritesError() throws Exception {
        RateLimitProperties.Rule rule =
                RateLimitProperties.Rule.of("login", "/v1/auth/**", List.of("POST"), 1, Duration.ofMinutes(1));
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(Clock.systemUTC(), 100);
        RateLimitFilter f = filter(props(true, List.of(rule)), limiter);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest first = get("POST", "/v1/auth/login", "");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        f.doFilter(first, res1, chain);
        verify(chain).doFilter(first, res1);

        MockHttpServletRequest second = get("POST", "/v1/auth/login", "");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        f.doFilter(second, res2, chain);

        assertThat(res2.getStatus()).isEqualTo(429);
        verify(chain, never()).doFilter(second, res2);
    }

    @Test
    @DisplayName("路径模式前的 contextPath 被剥离后再匹配")
    void contextPathStrippedBeforeMatching() throws Exception {
        RateLimitProperties.Rule rule =
                RateLimitProperties.Rule.of("login", "/api/auth/**", List.of("POST"), 10, Duration.ofMinutes(1));
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(Clock.systemUTC(), 100);
        RateLimitFilter f = filter(props(true, List.of(rule)), limiter);
        // contextPath=/app，完整 URI=/app/api/auth/login -> 剥离后 /api/auth/login 命中
        MockHttpServletRequest req = get("POST", "/app/api/auth/login", "/app");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        f.doFilter(req, res, chain);
        assertThat(res.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("remoteAddr 为 null/空白时归为 unknown（经构造器规则构造触发 clientAddress）")
    void nullRemoteAddrFallsBackToUnknown() throws Exception {
        RateLimitProperties.Rule rule =
                RateLimitProperties.Rule.of("login", "/v1/auth/**", List.of("POST"), 10, Duration.ofMinutes(1));
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(Clock.systemUTC(), 100);
        RateLimitFilter f = filter(props(true, List.of(rule)), limiter);
        MockHttpServletRequest req = get("POST", "/v1/auth/login", "");
        req.setRemoteAddr("   ");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        f.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    // ---------- 配置默认值 ----------

    @Test
    @DisplayName("Rule 默认值：空白 name 用 pathPattern，空 methods 用 POST")
    void ruleDefaults() {
        RateLimitProperties.Rule r = RateLimitProperties.Rule.of("  ", "/x/**", List.of(), -5, null);

        assertThat(r.name()).isEqualTo("/x/**");
        assertThat(r.methods()).containsExactly("POST");
        assertThat(r.capacity()).isEqualTo(30);
        assertThat(r.period()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("Rule：period 为 0/负数回退 1 分钟")
    void rulePeriodZeroAndNegative() {
        assertThat(RateLimitProperties.Rule.of("a", "/x", List.of("GET"), 5, Duration.ZERO)
                        .period())
                .isEqualTo(Duration.ofMinutes(1));
        assertThat(RateLimitProperties.Rule.of("a", "/x", List.of("GET"), 5, Duration.ofSeconds(-1))
                        .period())
                .isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("Rule：methods 为 null 时回退 POST")
    void ruleNullMethods() {
        RateLimitProperties.Rule r = RateLimitProperties.Rule.of("a", "/x", null, 5, Duration.ofMinutes(1));
        assertThat(r.methods()).containsExactly("POST");
    }

    @Test
    @DisplayName("Properties：rules 为 null、idleExpiry 非法、maxBuckets<=0 均回退默认")
    void propertiesDefaults() {
        RateLimitProperties p = new RateLimitProperties(true, true, null, Duration.ZERO, 0);
        assertThat(p.rules()).isEmpty();
        assertThat(p.idleExpiry()).isEqualTo(Duration.ofMinutes(10));
        assertThat(p.maxBuckets()).isEqualTo(100_000);
    }

    @Test
    @DisplayName("构造器过滤掉 pathPattern 为 null/空白的规则")
    void constructorFiltersBlankPathPattern() {
        RateLimitProperties.Rule blank =
                RateLimitProperties.Rule.of("blank", "  ", List.of("POST"), 5, Duration.ofMinutes(1));
        RateLimitProperties.Rule real =
                RateLimitProperties.Rule.of("real", "/v1/**", List.of("POST"), 5, Duration.ofMinutes(1));
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(Clock.systemUTC(), 100);
        RateLimitFilter f = filter(props(true, List.of(blank, real)), limiter);

        // 命中 /v1/anything 说明 real 规则已编译；blank 规则未参与
        MockHttpServletRequest req = get("POST", "/v1/anything", "");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        try {
            f.doFilter(req, res, chain);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertThat(res.getHeader("X-RateLimit-Limit")).isEqualTo("5");
    }
}
