// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.config;

import dev.strata.common.core.web.JsonBodyLimitFilter;
import dev.strata.common.core.web.WebHardeningProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import tools.jackson.databind.ObjectMapper;

/**
 * Registers the JSON body size limit filter. It runs early — below the security chain like the rate-limit filter —
 * so an oversize body is refused before authentication, message conversion or a BCrypt hash costs anything.
 */
@AutoConfiguration
@EnableConfigurationProperties(WebHardeningProperties.class)
public class WebHardeningAutoConfiguration {

    static final int FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 30;

    @Bean
    @ConditionalOnMissingBean(JsonBodyLimitFilter.class)
    JsonBodyLimitFilter jsonBodyLimitFilter(WebHardeningProperties properties, ObjectMapper objectMapper) {
        return new JsonBodyLimitFilter(properties, objectMapper);
    }

    @Bean
    FilterRegistrationBean<JsonBodyLimitFilter> jsonBodyLimitFilterRegistration(JsonBodyLimitFilter filter) {
        FilterRegistrationBean<JsonBodyLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(FILTER_ORDER);
        registration.addUrlPatterns("/*");
        registration.setName("jsonBodyLimitFilter");
        return registration;
    }
}
