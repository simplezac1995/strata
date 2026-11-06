// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.storage.local;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "strata.plugin.storage.local")
public record LocalStorageProperties(String baseDir) {

    public LocalStorageProperties {
        if (baseDir == null || baseDir.isBlank()) {
            baseDir = "data/strata-storage";
        }
    }
}
