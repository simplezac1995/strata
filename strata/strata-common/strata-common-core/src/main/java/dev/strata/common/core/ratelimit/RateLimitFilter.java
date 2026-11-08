// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.PathContainer;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Refuses a flood before it reaches anything expensive.
 *
 * <p>Two decisions shape it:
 *
 * <ul>
 *   <li><b>It runs before the security chain.</b> The point of a login limit is to refuse the request before a
 *       BCrypt hash is computed; a filter placed after authentication would still have paid that cost. The
 *       consequence is that the client key cannot be the authenticated player, so it is the client address —
 *       which is also the identity that matters for a flood.
 *   <li><b>The address is taken from {@code getRemoteAddr()}, never from a header.</b> {@code X-Forwarded-For} is
 *       attacker-controlled unless a trusted proxy sets it; a limiter keyed on a header an attacker can rotate
 *       limits nothing. Behind a load balancer, set {@code server.forward-headers-strategy} so the container
 *       resolves the real address, and do not expose the port directly.
 * </ul>
 *
 * <p>A rule set is policy: an empty rule list means this filter matches nothing and every request passes.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitProperties properties;
    private final RateLimiter limiter;
    private final RateLimitErrorWriter errorWriter;
    private final List<CompiledRule> compiled;

    public RateLimitFilter(RateLimitProperties properties, RateLimiter limiter, RateLimitErrorWriter errorWriter) {
        this.properties = properties;
        this.limiter = limiter;
        this.errorWriter = errorWriter;
        PathPatternParser parser = PathPatternParser.defaultInstance;
        this.compiled = properties.rules().stream()
                .filter(rule ->
                        rule.pathPattern() != null && !rule.pathPattern().isBlank())
                .map(rule -> new CompiledRule(rule, parser.parse(rule.pathPattern())))
                .toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!properties.enabled() || compiled.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        CompiledRule matched = match(request);
        if (matched == null) {
            chain.doFilter(request, response);
            return;
        }

        String key = matched.rule().name() + '|' + clientAddress(request);
        TokenBucketRateLimiter.Decision decision = limiter.tryAcquire(
                key,
                matched.rule().capacity(),
                matched.rule().period(),
                matched.rule().failureMode());
        response.setHeader("X-RateLimit-Limit", Integer.toString(matched.rule().capacity()));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(Math.max(decision.remaining(), 0)));

        if (!decision.allowed()) {
            log.warn(
                    "Rate limit '{}' reached for {} on {} {}; retry in {}s",
                    matched.rule().name(),
                    clientAddress(request),
                    request.getMethod(),
                    request.getRequestURI(),
                    decision.retryAfterSeconds());
            errorWriter.write(
                    request,
                    response,
                    matched.rule().name(),
                    decision.retryAfterSeconds(),
                    "Too many requests. Try again in " + decision.retryAfterSeconds() + " seconds.");
            return;
        }

        chain.doFilter(request, response);
    }

    private CompiledRule match(HttpServletRequest request) {
        String method = request.getMethod() == null ? "" : request.getMethod().toUpperCase(Locale.ROOT);
        PathContainer path = PathContainer.parsePath(pathWithinApplication(request));
        for (CompiledRule rule : compiled) {
            if (!rule.rule().methods().contains(method)) {
                continue;
            }
            if (rule.pattern().matches(path)) {
                return rule;
            }
        }
        return null;
    }

    /** Patterns are written against the application's own paths, so the context path is stripped first. */
    private static String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri;
    }

    /** The client's own address. A new source port must not mean a new bucket, and a header must not either. */
    private static String clientAddress(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }

    private record CompiledRule(RateLimitProperties.Rule rule, PathPattern pattern) {}
}
