// SPDX-License-Identifier: Apache-2.0
package dev.strata.example.plugin;

import dev.strata.plugin.api.status.PluginStatusProvider;

/**
 * Status contribution of the example plugin: {@code GET /api/v1/plugins/status} lists it under
 * {@code data.example} as soon as the auto-configuration is active.
 */
public class ExamplePluginStatusProvider implements PluginStatusProvider {

    private final ExamplePluginProperties properties;

    public ExamplePluginStatusProvider(ExamplePluginProperties properties) {
        this.properties = properties;
    }

    @Override
    public String key() {
        return "example";
    }

    @Override
    public String label() {
        return "Example plugin";
    }

    @Override
    public boolean available() {
        return properties.isEnabled();
    }

    @Override
    public String detail() {
        return properties.getDetail();
    }
}
