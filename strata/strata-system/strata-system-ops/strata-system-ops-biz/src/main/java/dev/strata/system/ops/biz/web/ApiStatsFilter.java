// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.system.ops.biz.service.ApiCallStatRecorder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Times every {@code /api/v1/**} request and hands a sample to {@link ApiCallStatRecorder} after the response is
 * committed. Sampling is 100% this iteration; the actual INSERT happens off the request thread so the stats write
 * never blocks the caller.
 *
 * <p>Registered through {@code ApiStatsFilterConfig} against {@code /api/v1/*} with a deliberately early order, so
 * the measured duration wraps the whole downstream filter chain (auth, routing, controller).
 */
public class ApiStatsFilter extends OncePerRequestFilter {

    private final ApiCallStatRecorder recorder;

    public ApiStatsFilter(ApiCallStatRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000L;
            recorder.record(
                    normalize(request.getRequestURI()),
                    request.getMethod(),
                    response.getStatus(),
                    durationMs,
                    clientIp(request));
        }
    }

    private static String normalize(String uri) {
        if (uri == null) {
            return "";
        }
        int query = uri.indexOf('?');
        return query >= 0 ? uri.substring(0, query) : uri;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
