// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Branch coverage for {@link JsonBodyLimitFilter}: oversize JSON rejected 413, JSON under the cap passed, non-JSON
 * bodies and disabled state passed through untouched.
 */
@DisplayName("JsonBodyLimitFilter JSON 请求体上限")
class JsonBodyLimitFilterTest {

    private static final int LIMIT = 1024;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebHardeningProperties props(boolean enabled) {
        return new WebHardeningProperties(enabled, LIMIT);
    }

    private JsonBodyLimitFilter filter(boolean enabled) {
        return new JsonBodyLimitFilter(props(enabled), objectMapper);
    }

    private static MockHttpServletRequest json(String method, String uri, int declaredLength) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        req.setContentType("application/json");
        req.setContent(new byte[declaredLength]);
        return req;
    }

    @Test
    @DisplayName("声明长度超限的 JSON 请求体返回 413，不继续链路")
    void oversizedJsonReturns413() throws Exception {
        MockHttpServletRequest req = json("POST", "/api/v1/things", LIMIT + 1);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter(true).doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(413);
        assertThat(res.getContentType()).contains("application/problem+json");
        verify(chain, never()).doFilter(req, res);
    }

    @Test
    @DisplayName("刚好等于上限的 JSON 请求体放行")
    void jsonAtLimitPasses() throws Exception {
        MockHttpServletRequest req = json("POST", "/api/v1/things", LIMIT);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter(true).doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("JSON 但声明长度为 -1（chunked）时不缓冲、放行，由容器 max-swallow-size 兜底")
    void unknownLengthJsonPassesThrough() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/things") {
            @Override
            public long getContentLengthLong() {
                return -1L;
            }
        };
        req.setContentType("application/json");
        req.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter(true).doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("超限但 Content-Type 非 JSON（form）放行")
    void oversizedNonJsonPasses() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/things");
        req.setContentType("application/x-www-form-urlencoded");
        req.setContent(new byte[LIMIT + 1]);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter(true).doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("过滤器关闭时一律放行")
    void disabledPassesEverything() throws Exception {
        MockHttpServletRequest req = json("POST", "/api/v1/things", LIMIT * 10);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter(false).doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("Properties：maxBytes 非法时回退 2MiB 默认")
    void invalidMaxBytesFallsBack() {
        WebHardeningProperties p = new WebHardeningProperties(true, 0);
        assertThat(p.jsonMaxBytes()).isEqualTo(2 * 1024 * 1024);
    }
}
