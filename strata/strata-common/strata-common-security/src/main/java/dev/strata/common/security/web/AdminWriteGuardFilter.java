// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.web;

import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.ProblemDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Origin allow-list + JSON-only enforcement for state-changing staff requests (P1-5).
 *
 * <p>Runs only on the admin surface ({@code /api/**}) and only for POST/PUT/PATCH/DELETE. Two cheap, stateless
 * signals stand in for a CSRF token on a token-in-JSON API:
 *
 * <ul>
 *   <li><b>Origin allow-list.</b> A browser always sends {@code Origin} on a cross-origin state change. If the
 *       allow-list is configured and the origin is not on it, the request is refused 403. A non-browser client
 *       (no {@code Origin} header) is not challenged here — it carries a bearer token and the auth chain already
 *       guards it.</li>
 *   <li><b>JSON-only.</b> A state-changing admin write that is not {@code application/json} is refused 415; the
 *       staff contract is JSON, and rejecting form-encoded writes removes the classic CSRF vector (a form POST cannot
 *       set an arbitrary content-type cross-origin without CORS preflight).</li>
 * </ul>
 *
 * <p>GET and OPTIONS are always allowed through (preflight and reads). When the allow-list is empty the Origin check
 * is skipped — local development; production must configure {@code strata.security.allowed-origins} non-empty.
 */
public class AdminWriteGuardFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AdminWriteGuardFilter.class);
    private static final AntPathMatcher ORIGIN_MATCHER = new AntPathMatcher();
    private static final List<String> WRITE_METHODS = List.of("POST", "PUT", "PATCH", "DELETE");

    private final AdminWriteGuardProperties properties;
    private final ObjectMapper objectMapper;
    private final SecurityMetrics metrics;

    public AdminWriteGuardFilter(
            AdminWriteGuardProperties properties, ObjectMapper objectMapper, SecurityMetrics metrics) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!properties.enabled() || !isAdminWrite(request)) {
            chain.doFilter(request, response);
            return;
        }

        // 1) Origin allow-list (browsers only; only when an allow-list is configured).
        String origin = request.getHeader("Origin");
        if (properties.originCheckEnabled() && StringUtils.hasText(origin) && !originAllowed(origin)) {
            log.warn(
                    "Rejecting state-changing {} {} from disallowed Origin {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    origin);
            metrics.securityRejection(HttpServletResponse.SC_FORBIDDEN);
            reject(response, ErrorCode.FORBIDDEN, "Origin not allowed for a state-changing request");
            return;
        }

        // 2) JSON-only. Multipart uploads are exempt; a write with no Content-Type (empty body, e.g. DELETE) is
        //    exempt — there is nothing to parse and nothing a cross-origin form could smuggle.
        String contentType = request.getContentType();
        boolean multipart =
                contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
        boolean json = contentType != null
                && contentType.toLowerCase(Locale.ROOT).startsWith(MediaType.APPLICATION_JSON_VALUE);
        if (contentType != null && !multipart && !json) {
            log.warn(
                    "Rejecting state-changing {} {} with non-JSON Content-Type {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    contentType);
            metrics.securityRejection(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            reject(response, ErrorCode.UNSUPPORTED_MEDIA_TYPE, "State-changing requests must be application/json");
            return;
        }

        chain.doFilter(request, response);
    }

    private static boolean isAdminWrite(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/")) {
            return false;
        }
        // The CSP violation receiver is a public browser endpoint: browsers POST it with an arbitrary Origin and a
        // non-JSON content type (application/csp-report), so it must be exempt from both checks.
        if (uri.startsWith("/api/v1/system/security/")) {
            return false;
        }
        String method = request.getMethod();
        return method != null && WRITE_METHODS.contains(method.toUpperCase(Locale.ROOT));
    }

    private boolean originAllowed(String origin) {
        for (String allowed : properties.allowedOrigins()) {
            if (StringUtils.hasText(allowed)
                    && (allowed.equals(origin) || ORIGIN_MATCHER.match(allowed.trim(), origin))) {
                return true;
            }
        }
        return false;
    }

    private void reject(HttpServletResponse response, ErrorCode errorCode, String detail) throws IOException {
        ProblemDetail body = ProblemDetails.of(errorCode, detail);
        response.setStatus(body.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
