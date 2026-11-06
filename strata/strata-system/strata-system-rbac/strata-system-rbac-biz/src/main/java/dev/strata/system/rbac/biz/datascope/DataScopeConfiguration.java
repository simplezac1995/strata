// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.datascope;

import dev.strata.system.rbac.biz.repository.DeptRepository;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import dev.strata.system.rbac.biz.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for data scope provider in the RBAC module.
 *
 * <p>Registers the {@link DefaultDataScopeProvider} as the default implementation
 * when no other {@link dev.strata.common.security.datascope.DataScopeProvider} bean is present.
 *
 * <p>To customize data scope resolution, create your own implementation:
 * <pre>{@code
 * @Component
 * public class CustomDataScopeProvider implements DataScopeProvider {
 *     // Custom implementation
 * }
 * }</pre>
 */
@Configuration
public class DataScopeConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DataScopeConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(dev.strata.common.security.datascope.DataScopeProvider.class)
    public dev.strata.common.security.datascope.DataScopeProvider dataScopeProvider(
            UserRepository userRepository, RoleRepository roleRepository, DeptRepository deptRepository) {
        log.info("Initializing default DataScopeProvider");
        return new DefaultDataScopeProvider(userRepository, roleRepository, deptRepository);
    }
}
