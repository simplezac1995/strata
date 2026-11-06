// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.monitor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that logs slow method execution in Service layer.
 *
 * <p>Wraps all public methods in classes ending with "Service" and logs execution time.
 * Methods exceeding the threshold (default 500ms) are logged at WARN level.
 *
 * <p>Output examples:
 * <pre>
 * WARN - Slow method: UserAppService.create() took 1234ms (threshold: 500ms)
 * DEBUG - Method: RoleAppService.update() took 45ms
 * ERROR - Method failed: MenuAppService.delete() took 123ms, error: ...
 * </pre>
 */
@Aspect
@Component
public class MethodPerformanceLogger {

    private static final Logger log = LoggerFactory.getLogger(MethodPerformanceLogger.class);

    /** Threshold in milliseconds for slow method warning. */
    private static final long SLOW_THRESHOLD_MS = 500;

    /**
     * Intercept all public methods in Service classes.
     */
    @Around("execution(* dev.strata..*Service.*(..)) && !execution(* dev.strata..*ServiceTest.*(..))")
    public Object logMethodPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;

            if (duration >= SLOW_THRESHOLD_MS) {
                log.warn("Slow method: {} took {}ms (threshold: {}ms)", methodName, duration, SLOW_THRESHOLD_MS);
            } else if (log.isDebugEnabled()) {
                log.debug("Method: {} took {}ms", methodName, duration);
            }

            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("Method failed: {} took {}ms, error: {}", methodName, duration, ex.getMessage());
            throw ex;
        }
    }
}
