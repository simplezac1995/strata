// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "strata.system.seed")
public record SeedProperties(String adminPassword) {

    public SeedProperties {
        if (adminPassword == null || adminPassword.isBlank()) {
            adminPassword = "admin123";
        }
    }
}
