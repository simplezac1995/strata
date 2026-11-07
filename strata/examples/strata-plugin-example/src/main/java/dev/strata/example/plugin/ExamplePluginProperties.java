// SPDX-License-Identifier: Apache-2.0
package dev.strata.example.plugin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Opt-in switch and display text for the example plugin. */
@ConfigurationProperties(prefix = "strata.example-plugin")
public class ExamplePluginProperties {

    /** Whether the example plugin is active (default: off, like every optional capability). */
    private boolean enabled = false;

    /** Detail string reported by the status endpoint; replace it with a real runtime probe. */
    private String detail = "example plugin is configured";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
