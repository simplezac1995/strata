// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/** Populates MDC traceId from the X-Trace-Id header (or generates one) and echoes it back on the response.
 *
 * <p>Order: Micrometer Tracing's observation filter creates the span (and fills MDC {@code traceId}
 * with its own random id) early; Spring Security runs at order {@code -100}. This filter runs
 * <em>after</em> the observation filter so the header value wins over the generated span id, yet
 * <em>before</em> the security chain so a 401 response still carries the caller-supplied trace id in
 * MDC (Problem Details and server logs).
 */
@Component
@Order(-200)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        // Mobile clients read X-Request-Id (not X-Trace-Id); both headers carry the same value.
        response.setHeader("X-Request-Id", traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
