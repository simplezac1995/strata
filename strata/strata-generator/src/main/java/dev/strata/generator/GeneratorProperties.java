// SPDX-License-Identifier: Apache-2.0
package dev.strata.generator;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "strata.generator")
public record GeneratorProperties(String outputDir) {

    public GeneratorProperties {
        if (outputDir == null || outputDir.isBlank()) {
            outputDir = "codegen-output";
        }
    }
}
