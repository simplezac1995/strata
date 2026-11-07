// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.config;

import dev.strata.system.ops.biz.service.ApiCallStatRecorder;
import dev.strata.system.ops.biz.web.ApiStatsFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers {@link ApiStatsFilter} against {@code /api/v1/*}. A {@link FilterRegistrationBean} is used (rather
 * than annotating the filter as a component) so the URL pattern and the order are explicit: early in the chain so
 * the measured duration wraps auth + routing, but after the bootstrapping filters.
 */
@Configuration
public class ApiStatsFilterConfig {

    @Bean
    public FilterRegistrationBean<ApiStatsFilter> apiStatsFilterRegistration(ApiCallStatRecorder recorder) {
        FilterRegistrationBean<ApiStatsFilter> registration =
                new FilterRegistrationBean<>(new ApiStatsFilter(recorder));
        registration.addUrlPatterns("/api/v1/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 100);
        registration.setName("apiStatsFilter");
        return registration;
    }
}
