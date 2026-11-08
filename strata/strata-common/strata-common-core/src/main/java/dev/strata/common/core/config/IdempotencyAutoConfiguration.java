// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.config;

import dev.strata.common.core.idempotency.IdempotencyErrorWriter;
import dev.strata.common.core.idempotency.IdempotencyFilter;
import dev.strata.common.core.idempotency.IdempotencyProperties;
import dev.strata.common.core.idempotency.IdempotencyPurgeJob;
import dev.strata.common.core.idempotency.IdempotencyScopeResolver;
import dev.strata.common.core.idempotency.IdempotencyStore;
import dev.strata.common.core.idempotency.JdbcIdempotencyStore;
import dev.strata.common.core.idempotency.ProblemDetailErrorWriter;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * Wires the generic idempotency filter.
 *
 * <p>The store resolves its {@link DataSource} through an {@link ObjectProvider} rather than through
 * {@code @ConditionalOnBean(DataSource.class)}: a bean condition would need this class to be ordered after
 * Boot's datasource auto-configuration, and that class lives in a package Boot 4 moved
 * ({@code org.springframework.boot.jdbc.autoconfigure}). Resolving the datasource while this bean is
 * instantiated has the same effect without coupling the framework module to those internals, and it keeps
 * a database-free deployment working: no datasource means no store, which means the filter stays out of
 * the chain instead of failing the context.
 */
@AutoConfiguration
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyAutoConfiguration {

    /** Null when there is no datasource: the framework must not force every module to have a database. */
    @Bean
    @ConditionalOnClass(JdbcTemplate.class)
    @ConditionalOnMissingBean(IdempotencyStore.class)
    IdempotencyStore idempotencyStore(ObjectProvider<DataSource> dataSourceProvider) {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        return dataSource == null ? null : new JdbcIdempotencyStore(new JdbcTemplate(dataSource));
    }

    @Bean
    @ConditionalOnMissingBean(IdempotencyErrorWriter.class)
    IdempotencyErrorWriter idempotencyErrorWriter(ObjectMapper objectMapper) {
        return new ProblemDetailErrorWriter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(IdempotencyScopeResolver.class)
    IdempotencyScopeResolver idempotencyScopeResolver() {
        return IdempotencyScopeResolver.principalOrRemoteAddress();
    }

    /**
     * Registered as a plain servlet filter on purpose: Spring Boot gives it the lowest precedence, which
     * puts it after the security chain. That ordering matters twice: the scope needs an authenticated
     * principal, and an anonymous caller must not be able to reserve a user's key.
     */
    @Bean
    @ConditionalOnMissingBean(IdempotencyFilter.class)
    IdempotencyFilter idempotencyFilter(
            IdempotencyProperties properties,
            ObjectProvider<IdempotencyStore> storeProvider,
            IdempotencyErrorWriter errorWriter,
            IdempotencyScopeResolver scopeResolver) {
        IdempotencyStore store = storeProvider.getIfAvailable();
        if (store == null) {
            // No store (no datasource): register nothing rather than a filter that could only reject.
            return null;
        }
        return new IdempotencyFilter(properties, store, errorWriter, scopeResolver);
    }

    @Bean
    @ConditionalOnProperty(prefix = "strata.idempotency", name = "purge-enabled", matchIfMissing = true)
    @ConditionalOnMissingBean(IdempotencyPurgeJob.class)
    IdempotencyPurgeJob idempotencyPurgeJob(ObjectProvider<IdempotencyStore> storeProvider) {
        IdempotencyStore store = storeProvider.getIfAvailable();
        return store == null ? null : new IdempotencyPurgeJob(store);
    }
}
