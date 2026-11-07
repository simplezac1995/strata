// SPDX-License-Identifier: Apache-2.0
package dev.strata.example.plugin;

import dev.strata.plugin.api.status.PluginStatusProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Golden path for a plugin: a normal Spring Boot auto-configuration, off by default, registered through
 * {@code META-INF/spring/...AutoConfiguration.imports}. Adding the jar plus
 * {@code strata.example-plugin.enabled=true} is the only consumer-side change.
 */
@AutoConfiguration
@EnableConfigurationProperties(ExamplePluginProperties.class)
@ConditionalOnProperty(prefix = "strata.example-plugin", name = "enabled", havingValue = "true")
public class ExamplePluginAutoConfiguration {

    @Bean
    public PluginStatusProvider examplePluginStatusProvider(ExamplePluginProperties properties) {
        return new ExamplePluginStatusProvider(properties);
    }
}
