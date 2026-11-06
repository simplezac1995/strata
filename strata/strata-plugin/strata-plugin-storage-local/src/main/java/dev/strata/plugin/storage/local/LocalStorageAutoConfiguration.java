// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.storage.local;

import dev.strata.plugin.api.storage.ObjectStorage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "strata.plugin.storage.local", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(LocalStorageProperties.class)
public class LocalStorageAutoConfiguration {

    @Bean
    ObjectStorage localObjectStorage(LocalStorageProperties properties) {
        return new LocalObjectStorage(properties);
    }
}
