// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.tenant;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
@EnableConfigurationProperties(TenantProperties.class)
public class TenantAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "strata.tenant", name = "row-level-enabled", havingValue = "true")
    FilterRegistrationBean<TenantContextFilter> tenantContextFilter(TenantProperties properties) {
        FilterRegistrationBean<TenantContextFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new TenantContextFilter(properties));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return bean;
    }
}
