// SPDX-License-Identifier: Apache-2.0
package dev.strata;

import org.springframework.modulith.Modulith;

/**
 * Package anchor for the Spring Modulith module model.
 *
 * <p>The open-source tree has no deployable application module of its own: the runnable example is a repackaged Spring
 * Boot executable and cannot be used as a plain test dependency. The module model is therefore created from this
 * test-scoped class, which only exists so {@code ApplicationModules} can be pointed at the {@code dev.strata} root
 * package. Spring Modulith only derives the module model from a type annotated with {@code @Modulith} /
 * {@code @Modulithic} / {@code @SpringBootApplication}.
 */
@Modulith
public final class ArchitectureMarker {

    private ArchitectureMarker() {}
}
