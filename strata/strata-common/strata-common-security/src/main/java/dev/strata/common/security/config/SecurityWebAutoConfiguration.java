// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.config;

import dev.strata.common.security.web.AdminWriteGuardFilter;
import dev.strata.common.security.web.AdminWriteGuardProperties;
import dev.strata.common.security.web.CookieHardeningFilter;
import dev.strata.common.security.web.CookieSecurityProperties;
import dev.strata.common.security.web.CspReportController;
import dev.strata.common.security.web.SecurityMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import tools.jackson.databind.ObjectMapper;

/**
 * Wires the browser-facing hardening layer: the admin write guard (Origin + JSON-only), the cookie hardening wrapper,
 * the security metrics counters and the CSP violation report receiver.
 *
 * <p>Kept separate from {@link SecurityAutoConfiguration} so the filter-chain concerns (authz, JWT) stay where they
 * are and these cross-cutting guards add on top.
 */
@AutoConfiguration
@EnableConfigurationProperties({AdminWriteGuardProperties.class, CookieSecurityProperties.class})
public class SecurityWebAutoConfiguration {

    /** Above the rate-limit filter is fine; these checks are cheaper than a flood rule. */
    static final int ADMIN_GUARD_ORDER = Ordered.HIGHEST_PRECEDENCE + 40;

    static final int COOKIE_HARDENING_ORDER = Ordered.LOWEST_PRECEDENCE - 50;

    @Bean
    @ConditionalOnMissingBean
    SecurityMetrics securityMetrics(ObjectProvider<MeterRegistry> registryProvider) {
        MeterRegistry registry = registryProvider.getIfAvailable(SimpleMeterRegistry::new);
        return new SecurityMetrics(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    AdminWriteGuardFilter adminWriteGuardFilter(
            AdminWriteGuardProperties properties, ObjectMapper objectMapper, SecurityMetrics metrics) {
        return new AdminWriteGuardFilter(properties, objectMapper, metrics);
    }

    @Bean
    FilterRegistrationBean<AdminWriteGuardFilter> adminWriteGuardFilterRegistration(AdminWriteGuardFilter filter) {
        FilterRegistrationBean<AdminWriteGuardFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(ADMIN_GUARD_ORDER);
        registration.addUrlPatterns("/*");
        registration.setName("adminWriteGuardFilter");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    CookieHardeningFilter cookieHardeningFilter(CookieSecurityProperties properties) {
        return new CookieHardeningFilter(properties);
    }

    @Bean
    FilterRegistrationBean<CookieHardeningFilter> cookieHardeningFilterRegistration(CookieHardeningFilter filter) {
        FilterRegistrationBean<CookieHardeningFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(COOKIE_HARDENING_ORDER);
        registration.addUrlPatterns("/*");
        registration.setName("cookieHardeningFilter");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    CspReportController cspReportController(SecurityMetrics metrics) {
        return new CspReportController(metrics);
    }
}
