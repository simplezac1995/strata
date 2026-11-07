// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.ratelimit.RateLimitProperties;
import dev.strata.common.core.ratelimit.RateLimiter;
import dev.strata.common.core.ratelimit.TokenBucketRateLimiter;
import dev.strata.common.core.ratelimit.TokenBucketRateLimiter.Decision;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.security.Principal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class RateLimitAspectTest {

    private RateLimitProperties enabled;
    private RateLimitProperties disabled;

    /** Method-level fixtures. */
    static class Target {
        @RateLimit(limit = 2, window = 60, key = "login", perIp = false, perUser = false)
        public Object limited() {
            return "ok";
        }

        @RateLimit(limit = 1, window = 60, key = "one", perIp = false, perUser = false)
        public Object oneShot() {
            return "ok";
        }

        @RateLimit(window = 60, perIp = true, perUser = false)
        public Object byIp() {
            return "ok";
        }

        @RateLimit(window = 60, key = "shared", perIp = false, perUser = false)
        public Object shared() {
            return "ok";
        }

        @RateLimit(window = 60, key = "user-ep", perIp = false, perUser = true)
        public Object byUser() {
            return "ok";
        }

        public Object plain() {
            return "ok";
        }
    }

    /** Class-level fixture. */
    @RateLimit(limit = 1, window = 60, key = "class-ep", perIp = false, perUser = false)
    static class ClassTarget {
        public Object method() {
            return "ok";
        }
    }

    /** A clock whose instant the test can advance, so refill timing is deterministic. */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }
    }

    @BeforeEach
    void setUp() {
        enabled = new RateLimitProperties(true, true, List.of(), Duration.ofMinutes(10), 100_000);
        disabled = new RateLimitProperties(false, true, List.of(), Duration.ofMinutes(10), 100_000);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    private ProceedingJoinPoint joinPointFor(Object target, String methodName) throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = target.getClass().getMethod(methodName);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getTarget()).thenReturn(target);
        when(signature.getMethod()).thenReturn(method);
        doReturn(method.getDeclaringClass().getName()).when(signature).getDeclaringTypeName();
        when(signature.getName()).thenReturn(methodName);
        when(pjp.proceed()).thenReturn("ok");
        return pjp;
    }

    private RateLimit annotationOn(Object target, String methodName) throws Exception {
        return target.getClass().getMethod(methodName).getAnnotation(RateLimit.class);
    }

    /** A real in-memory bucket (the production default) sharing the {@code RateLimiter} seam. */
    private TokenBucketRateLimiter memoryLimiter() {
        return new TokenBucketRateLimiter(Clock.systemUTC(), 10_000);
    }

    @Test
    void disabledNeverThrottles() throws Throwable {
        RateLimiter limiter = mock(RateLimiter.class);
        RateLimitAspect aspect = new RateLimitAspect(disabled, limiter);
        ProceedingJoinPoint pjp = joinPointFor(new Target(), "limited");

        for (int i = 0; i < 5; i++) {
            assertThat(aspect.around(pjp, null)).isEqualTo("ok");
        }
        verify(pjp, times(5)).proceed();
        verify(limiter, never()).tryAcquire(anyString(), anyInt(), any());
    }

    @Test
    void annotationsDisabledNeverThrottles() throws Throwable {
        RateLimiter limiter = mock(RateLimiter.class);
        RateLimitProperties annotationsOff =
                new RateLimitProperties(true, false, List.of(), Duration.ofMinutes(10), 100_000);
        RateLimitAspect aspect = new RateLimitAspect(annotationsOff, limiter);
        ProceedingJoinPoint pjp = joinPointFor(new Target(), "limited");

        for (int i = 0; i < 5; i++) {
            assertThat(aspect.around(pjp, null)).isEqualTo("ok");
        }
        verify(pjp, times(5)).proceed();
        verify(limiter, never()).tryAcquire(anyString(), anyInt(), any());
    }

    @Test
    void noAnnotationProceeds() throws Throwable {
        RateLimiter limiter = mock(RateLimiter.class);
        RateLimitAspect aspect = new RateLimitAspect(enabled, limiter);
        ProceedingJoinPoint pjp = joinPointFor(new Target(), "plain");

        assertThat(aspect.around(pjp, null)).isEqualTo("ok");
        verify(pjp).proceed();
        verify(limiter, never()).tryAcquire(anyString(), anyInt(), any());
    }

    @Test
    void rejectsAfterLimitExhausted() throws Throwable {
        RateLimitAspect aspect = new RateLimitAspect(enabled, memoryLimiter());
        ProceedingJoinPoint pjp = joinPointFor(new Target(), "limited");

        assertThat(aspect.around(pjp, null)).isEqualTo("ok");
        assertThat(aspect.around(pjp, null)).isEqualTo("ok");
        assertThatThrownBy(() -> aspect.around(pjp, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Rate limit exceeded");
        verify(pjp, times(2)).proceed();
    }

    @Test
    void respectsDirectlyBoundAnnotation() throws Throwable {
        Target target = new Target();
        RateLimitAspect aspect = new RateLimitAspect(enabled, memoryLimiter());
        ProceedingJoinPoint pjp = joinPointFor(target, "limited");

        RateLimit bound = annotationOn(target, "limited");
        assertThat(aspect.around(pjp, bound)).isEqualTo("ok");
        assertThat(aspect.around(pjp, bound)).isEqualTo("ok");
        assertThatThrownBy(() -> aspect.around(pjp, bound)).isInstanceOf(BusinessException.class);
    }

    @Test
    void fallsBackToClassLevelAnnotation() throws Throwable {
        RateLimitAspect aspect = new RateLimitAspect(enabled, memoryLimiter());
        ProceedingJoinPoint pjp = joinPointFor(new ClassTarget(), "method");

        assertThat(aspect.around(pjp, null)).isEqualTo("ok");
        assertThatThrownBy(() -> aspect.around(pjp, null)).isInstanceOf(BusinessException.class);
        verify(pjp, times(1)).proceed();
    }

    @Test
    void emptyKeyUsesMethodNameAndUnknownIp() throws Throwable {
        RateLimiter limiter = mock(RateLimiter.class);
        when(limiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(new Decision(true, 1, 0));
        RateLimitAspect aspect = new RateLimitAspect(enabled, limiter);
        aspect.around(joinPointFor(new Target(), "byIp"), null);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(limiter).tryAcquire(key.capture(), anyInt(), any());
        assertThat(key.getValue()).isEqualTo("ratelimit:" + Target.class.getName() + ".byIp:ip=unknown");
    }

    @Test
    void forgedForwardedHeaderDoesNotChangeBucketKey() throws Throwable {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1, 2.2.2.2");
        when(request.getHeader("X-Real-IP")).thenReturn("3.3.3.3");
        when(request.getRemoteAddr()).thenReturn("9.9.9.9");
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        RateLimiter limiter = mock(RateLimiter.class);
        when(limiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(new Decision(true, 1, 0));
        RateLimitAspect aspect = new RateLimitAspect(enabled, limiter);
        aspect.around(joinPointFor(new Target(), "byIp"), null);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(limiter).tryAcquire(key.capture(), anyInt(), any());
        // Must be the socket address, never the attacker-supplied headers.
        assertThat(key.getValue()).endsWith(":ip=9.9.9.9");
        assertThat(key.getValue()).doesNotContain("1.1.1.1").doesNotContain("3.3.3.3");
    }

    @Test
    void perIpUsesRemoteAddrWhenPresent() throws Throwable {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("4.4.4.4");
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        RateLimiter limiter = mock(RateLimiter.class);
        when(limiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(new Decision(true, 1, 0));
        RateLimitAspect aspect = new RateLimitAspect(enabled, limiter);
        aspect.around(joinPointFor(new Target(), "byIp"), null);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(limiter).tryAcquire(key.capture(), anyInt(), any());
        assertThat(key.getValue()).endsWith(":ip=4.4.4.4");
    }

    @Test
    void perUserAnonymousWhenUnauthenticated() throws Throwable {
        RateLimiter limiter = mock(RateLimiter.class);
        when(limiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(new Decision(true, 1, 0));
        RateLimitAspect aspect = new RateLimitAspect(enabled, limiter);
        aspect.around(joinPointFor(new Target(), "byUser"), null);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(limiter).tryAcquire(key.capture(), anyInt(), any());
        assertThat(key.getValue()).endsWith(":user=anonymous");
    }

    @Test
    void perUserUsesPrincipalName() throws Throwable {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("bob");
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        RateLimiter limiter = mock(RateLimiter.class);
        when(limiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(new Decision(true, 1, 0));
        RateLimitAspect aspect = new RateLimitAspect(enabled, limiter);
        aspect.around(joinPointFor(new Target(), "byUser"), null);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(limiter).tryAcquire(key.capture(), anyInt(), any());
        assertThat(key.getValue()).endsWith(":user=bob");
    }

    @Test
    void perUserUsesPrincipalToString() throws Throwable {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("str-principal");
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        RateLimiter limiter = mock(RateLimiter.class);
        when(limiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(new Decision(true, 1, 0));
        RateLimitAspect aspect = new RateLimitAspect(enabled, limiter);
        aspect.around(joinPointFor(new Target(), "byUser"), null);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(limiter).tryAcquire(key.capture(), anyInt(), any());
        assertThat(key.getValue()).endsWith(":user=str-principal");
    }

    @Test
    void bucketRefillsAfterWindowElapses() throws Throwable {
        MutableClock clock = new MutableClock(Instant.now());
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(clock, 10_000);
        RateLimitAspect aspect = new RateLimitAspect(enabled, limiter);
        ProceedingJoinPoint pjp = joinPointFor(new Target(), "oneShot");

        assertThat(aspect.around(pjp, null)).isEqualTo("ok");
        assertThatThrownBy(() -> aspect.around(pjp, null)).isInstanceOf(BusinessException.class);

        // Advance past the refill window, then the bucket refills and the request is served again.
        clock.advance(Duration.ofSeconds(61));
        assertThat(aspect.around(pjp, null)).isEqualTo("ok");
        verify(pjp, times(2)).proceed();
    }

    @Test
    void sharedBucketAcrossAspects() throws Throwable {
        // Both aspects share ONE limiter instance, exactly as two app replicas share one Redis bucket.
        TokenBucketRateLimiter shared = new TokenBucketRateLimiter(Clock.systemUTC(), 10_000);
        RateLimitAspect first = new RateLimitAspect(enabled, shared);
        RateLimitAspect second = new RateLimitAspect(enabled, shared);
        ProceedingJoinPoint pjp = joinPointFor(new Target(), "shared");

        // limit default = 100; exhaust it across both aspects and the 101st call must be rejected.
        for (int i = 0; i < 100; i++) {
            RateLimitAspect use = (i % 2 == 0) ? first : second;
            assertThat(use.around(pjp, null)).isEqualTo("ok");
        }
        assertThatThrownBy(() -> second.around(pjp, null)).isInstanceOf(BusinessException.class);
    }
}
