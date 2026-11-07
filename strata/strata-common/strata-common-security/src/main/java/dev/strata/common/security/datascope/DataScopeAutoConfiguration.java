// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.datascope;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for data scope (row-level permission).
 *
 * <p>Enables:
 * <ul>
 *   <li>{@link DataScope} annotation support via AOP</li>
 *   <li>{@link DataScopeContext} for accessing current scope</li>
 *   <li>{@link DataScopeProvider} SPI for resolving user scopes</li>
 * </ul>
 *
 * <p>Requires a {@link DataScopeProvider} bean to be present.
 * Applications should implement this interface to provide user-specific data scope resolution.
 *
 * <p>Enabled by default, can be disabled via:
 * <pre>{@code
 * strata:
 *   security:
 *     data-scope:
 *       enabled: false
 * }</pre>
 */
@Configuration
@ConditionalOnProperty(
        prefix = "strata.security.data-scope",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@ConditionalOnBean(DataScopeProvider.class)
public class DataScopeAutoConfiguration {

    @Bean
    public DataScopeAspect dataScopeAspect(DataScopeProvider dataScopeProvider) {
        return new DataScopeAspect(dataScopeProvider);
    }
}
