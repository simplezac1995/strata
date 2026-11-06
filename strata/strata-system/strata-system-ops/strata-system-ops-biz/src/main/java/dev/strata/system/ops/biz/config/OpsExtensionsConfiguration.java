// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.config;

import dev.strata.common.core.spi.AuditChangeApplier;
import dev.strata.common.core.spi.BackupExecutor;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default implementations for the operations extension points: backups and audit write-back need deployment-specific
 * infrastructure, so without an override they are recorded but not executed.
 */
@Configuration(proxyBeanMethods = false)
public class OpsExtensionsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OpsExtensionsConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(BackupExecutor.class)
    BackupExecutor noopBackupExecutor() {
        return requestedPath -> {
            log.info("backup requested for {} but no BackupExecutor is configured", requestedPath);
            return Optional.empty();
        };
    }

    @Bean
    @ConditionalOnMissingBean(AuditChangeApplier.class)
    AuditChangeApplier noopAuditChangeApplier() {
        return (entityName, entityId, changesJson, operator) -> {
            log.info(
                    "audit change replay requested for {}/{} but no AuditChangeApplier is configured",
                    entityName,
                    entityId);
            return false;
        };
    }
}
