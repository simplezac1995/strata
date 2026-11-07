// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Branch coverage for {@link AdminWriteGuardFilter}: Origin allow-list (403), JSON-only (415), GET/OPTIONS
 * pass-through, and the empty allow-list dev permissive mode.
 */
class AdminWriteGuardFilterTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final SecurityMetrics metrics = new SecurityMetrics(meterRegistry);

    private AdminWriteGuardFilter filter(List<String> allowedOrigins) {
        return new AdminWriteGuardFilter(
                new AdminWriteGuardProperties(true, allowedOrigins), new ObjectMapper(), metrics);
    }

    private static MockHttpServletRequest req(String method, String uri, String origin, String contentType) {
        MockHttpServletRequest r = new MockHttpServletRequest(method, uri);
        if (origin != null) {
            r.addHeader("Origin", origin);
        }
        if (contentType != null) {
            r.setContentType(contentType);
        }
        return r;
    }

    @Test
    void allowedOriginAndJsonPasses() throws Exception {
        MockHttpServletRequest r = req("POST", "/api/v1/users", "https://admin.example.com", "application/json");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter(List.of("https://admin.example.com")).doFilter(r, res, chain);

        verify(chain).doFilter(r, res);
    }

    @Test
    void disallowedOriginReturns403() throws Exception {
        MockHttpServletRequest r = req("POST", "/api/v1/users", "https://evil.example.com", "application/json");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter(List.of("https://admin.example.com")).doFilter(r, res, chain);

        assertThat(res.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(r, res);
        assertThat(meterRegistry
                        .find("security_rejection_total")
                        .tag("status", "403")
                        .counter()
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    void wildcardOriginMatches() throws Exception {
        MockHttpServletRequest r = req("POST", "/api/v1/users", "http://localhost:5173", "application/json");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter(List.of("http://localhost:*")).doFilter(r, res, chain);

        verify(chain).doFilter(r, res);
    }

    @Test
    void nonJsonWriteReturns415() throws Exception {
        MockHttpServletRequest r = req("POST", "/api/v1/users", "https://admin.example.com", "text/plain");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter(List.of("https://admin.example.com")).doFilter(r, res, chain);

        assertThat(res.getStatus()).isEqualTo(415);
        verify(chain, never()).doFilter(r, res);
    }

    @Test
    void multipartExempt() throws Exception {
        MockHttpServletRequest r =
                req("POST", "/api/v1/files", "https://admin.example.com", "multipart/form-data; boundary=x");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter(List.of("https://admin.example.com")).doFilter(r, res, chain);

        verify(chain).doFilter(r, res);
    }

    @Test
    void getPassesThrough() throws Exception {
        MockHttpServletRequest r = req("GET", "/api/v1/users", "https://evil.example.com", null);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter(List.of("https://admin.example.com")).doFilter(r, res, chain);

        verify(chain).doFilter(r, res);
    }

    @Test
    void nonApiSurfaceIgnored() throws Exception {
        MockHttpServletRequest r = req("POST", "/v1/wallet", "https://evil.example.com", "text/plain");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter(List.of("https://admin.example.com")).doFilter(r, res, chain);

        verify(chain).doFilter(r, res);
    }

    @Test
    void cspReportEndpointExempt() throws Exception {
        MockHttpServletRequest r =
                req("POST", "/api/v1/system/security/csp-report", "https://evil.example.com", "application/csp-report");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter(List.of("https://admin.example.com")).doFilter(r, res, chain);

        verify(chain).doFilter(r, res);
    }

    @Test
    void emptyAllowListSkipsOriginCheck() throws Exception {
        MockHttpServletRequest r = req("POST", "/api/v1/users", "https://anything.example.com", "application/json");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter(List.of()).doFilter(r, res, chain);

        verify(chain).doFilter(r, res);
    }
}
