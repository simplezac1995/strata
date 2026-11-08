// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.ratelimit;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.ratelimit.RateLimitProperties;
import dev.strata.common.core.ratelimit.RateLimiter;
import dev.strata.common.core.ratelimit.TokenBucketRateLimiter.Decision;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.Duration;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspect that enforces rate limiting on methods annotated with {@link RateLimit}.
 *
 * <p>Supports per-IP and per-User rate limiting dimensions. Buckets are owned by the injected {@link
 * RateLimiter} seam: by default that is the in-memory {@code TokenBucketRateLimiter}, and when the deployment
 * enables Redis ({@code strata.data.redis.enabled=true}) it is transparently swapped for the {@code
 * RedissonRateLimiter} that the path-level {@code RateLimitFilter} already uses. Method-level annotations
 * therefore share the same Redis budget and the same key prefix as the path rules — no second, easily-forgotten
 * opt-in and no divergent bucket semantics.
 *
 * <p><b>The client address is taken from {@code getRemoteAddr()}, never from a forwarded header.</b> {@code
 * X-Forwarded-For} is attacker-controlled unless a trusted proxy sets it; a bucket keyed on a header an attacker
 * can rotate limits nothing. This mirrors {@code RateLimitFilter.clientAddress}. Behind a load balancer set
 * {@code server.forward-headers-strategy=native} (as the prod profile already does) so the container resolves the
 * real address itself, and keep the app port off the public interface.
 *
 * <p>When the limit is exceeded, throws {@link BusinessException} with {@link ErrorCode#TOO_MANY_REQUESTS}.
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final RateLimitProperties properties;
    private final RateLimiter limiter;

    public RateLimitAspect(RateLimitProperties properties, RateLimiter limiter) {
        this.properties = properties;
        this.limiter = limiter;
    }

    @Around("@annotation(rateLimit) || @within(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // Same switch as the rules-based filter: when strata.rate-limit.enabled is false, annotated
        // endpoints must not be throttled either (a disabled feature must not still reject traffic).
        // annotations-enabled is the narrower switch integration suites use: they sign in hundreds of times from one
        // address inside one JVM, so these per-endpoint buckets would answer 429 to the tests themselves.
        if (!properties.enabled() || !properties.annotationsEnabled()) {
            return joinPoint.proceed();
        }
        if (rateLimit == null) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            rateLimit = signature.getMethod().getAnnotation(RateLimit.class);
            if (rateLimit == null) {
                rateLimit = joinPoint.getTarget().getClass().getAnnotation(RateLimit.class);
            }
        }
        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        String key = buildKey(joinPoint, rateLimit);
        Decision decision =
                limiter.tryAcquire(key, rateLimit.limit(), Duration.ofSeconds(Math.max(rateLimit.window(), 1)));
        if (!decision.allowed()) {
            log.warn("Rate limit exceeded for key: {} (limit: {}/{}s)", key, rateLimit.limit(), rateLimit.window());
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "Rate limit exceeded. Try again later.");
        }

        return joinPoint.proceed();
    }

    private String buildKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        StringBuilder key = new StringBuilder("ratelimit:");
        if (!rateLimit.key().isEmpty()) {
            key.append(rateLimit.key());
        } else {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            key.append(signature.getDeclaringTypeName()).append('.').append(signature.getName());
        }
        if (rateLimit.perIp()) {
            key.append(":ip=").append(getClientIp());
        }
        if (rateLimit.perUser()) {
            key.append(":user=").append(getCurrentUserId());
        }
        return key.toString();
    }

    /**
     * The caller's own socket address. A forged {@code X-Forwarded-For} / {@code X-Real-IP} header must not
     * mean a fresh bucket, exactly as {@code RateLimitFilter} enforces. The real client address behind a proxy
     * is resolved by the container through {@code server.forward-headers-strategy=native}, which rewrites {@code
     * getRemoteAddr()} — it is not something this layer parses from an untrusted header.
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Principal userPrincipal) {
            return userPrincipal.getName();
        }
        if (principal != null) {
            return principal.toString();
        }
        return "anonymous";
    }
}
