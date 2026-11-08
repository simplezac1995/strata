// SPDX-License-Identifier: Apache-2.0
package dev.strata.example.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.plugin.api.status.PluginStatusProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ExamplePluginAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ExamplePluginAutoConfiguration.class));

    @Test
    void isOffByDefault() {
        runner.run(context -> assertThat(context).doesNotHaveBean(PluginStatusProvider.class));
    }

    @Test
    void registersAStatusProviderWhenEnabled() {
        runner.withPropertyValues("strata.example-plugin.enabled=true", "strata.example-plugin.detail=ready")
                .run(context -> {
                    assertThat(context).hasSingleBean(PluginStatusProvider.class);
                    PluginStatusProvider provider = context.getBean(PluginStatusProvider.class);
                    assertThat(provider.key()).isEqualTo("example");
                    assertThat(provider.available()).isTrue();
                    assertThat(provider.detail()).isEqualTo("ready");
                });
    }
}
