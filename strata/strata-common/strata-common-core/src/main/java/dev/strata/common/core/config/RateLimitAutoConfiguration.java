// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.config;

import dev.strata.common.core.ratelimit.ProblemDetailRateLimitWriter;
import dev.strata.common.core.ratelimit.RateLimitErrorWriter;
import dev.strata.common.core.ratelimit.RateLimitFilter;
import dev.strata.common.core.ratelimit.RateLimitProperties;
import dev.strata.common.core.ratelimit.RateLimiter;
import dev.strata.common.core.ratelimit.TokenBucketRateLimiter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import tools.jackson.databind.ObjectMapper;

/**
 * Wires the rate limit filter.
 *
 * <p>The registration order is the only subtle part: this filter must run <b>before</b> the security chain, so a
 * throttled login is refused before a BCrypt hash is computed. Spring Security registers its chain at order
 * {@code -100}, so anything below that wins. The filter is registered through a
 * {@link FilterRegistrationBean} rather than as a bare bean, because a bare bean would be appended after the
 * security chain where the expensive work has already happened.
 */
@AutoConfiguration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitAutoConfiguration {

    /** Below Spring Security's {@code -100}, above the container's own filters. */
    static final int FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 20;

    @Bean
    @ConditionalOnMissingBean(RateLimitErrorWriter.class)
    RateLimitErrorWriter rateLimitErrorWriter(ObjectMapper objectMapper) {
        return new ProblemDetailRateLimitWriter(objectMapper);
    }

    /**
     * The default limiter is the in-memory token bucket. A distributed (Redis) implementation can replace it by
     * contributing a {@link RateLimiter} bean; this definition then backs off via {@link ConditionalOnMissingBean}.
     */
    @Bean
    @ConditionalOnMissingBean(RateLimiter.class)
    RateLimiter tokenBucketRateLimiter(RateLimitProperties properties) {
        return new TokenBucketRateLimiter(Clock.systemUTC(), properties.maxBuckets());
    }

    @Bean
    @ConditionalOnMissingBean(RateLimitFilter.class)
    RateLimitFilter rateLimitFilter(
            RateLimitProperties properties, RateLimiter limiter, RateLimitErrorWriter errorWriter) {
        return new RateLimitFilter(properties, limiter, errorWriter);
    }

    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(FILTER_ORDER);
        registration.addUrlPatterns("/*");
        registration.setName("rateLimitFilter");
        return registration;
    }
}
