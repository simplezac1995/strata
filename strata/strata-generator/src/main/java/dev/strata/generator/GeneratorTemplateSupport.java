// SPDX-License-Identifier: Apache-2.0
package dev.strata.generator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Template resource access for codegen: loads the UTF-8 templates under {@code
 * classpath:/templates/}. Shared by {@link GeneratorBackendRenderer} and {@link
 * GeneratorFrontendRenderer}.
 */
final class GeneratorTemplateSupport {

    String loadTemplate(String name) {
        try (var in = getClass().getResourceAsStream("/templates/" + name)) {
            if (in == null) {
                throw new IllegalStateException("Template not found on classpath: /templates/" + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load template: " + name, ex);
        }
    }
}
