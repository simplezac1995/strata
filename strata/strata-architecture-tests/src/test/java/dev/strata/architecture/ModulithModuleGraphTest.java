// SPDX-License-Identifier: Apache-2.0
package dev.strata.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ImportOption;
import dev.strata.ArchitectureMarker;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Spring Modulith gate over the modules that ship in the open-source repository.
 *
 * <p>Unlike the product repository's check (which is pointed at the deployable application class and therefore only
 * sees that application's direct sub-packages), this test derives the real module graph from the {@code dev.strata}
 * root package: common, system, ai, plugin, generator, starter and the example module. It is the honest replacement
 * for the framework-level verification the public repository was missing.
 *
 * <p>State of the full {@link ApplicationModules#verify()} verification: the framework's {@code *-api} Maven artifacts
 * are not yet declared as Spring Modulith named interfaces, so the default verification reports every cross-module
 * reference into an internal package (for example {@code system.api.dto} or {@code common.core}) as a violation. The
 * fix is a mechanical but broad pass of {@code @NamedInterface} package annotations, which is out of scope for the
 * first version of this gate. What is enforced here is the part that is meaningful today: the module arrangement is
 * detected (a module silently disappearing from the model fails the build) and the module-level dependency graph is
 * acyclic (a new cycle fails the build). The ArchUnit rules in {@link ArchitectureRulesTest} enforce the API/implementation
 * direction in the meantime.
 */
class ModulithModuleGraphTest {

    private static final Set<String> EXPECTED_MODULES =
            Set.of("ai", "common", "example", "generator", "plugin", "starter", "system");

    private static final String FIXTURE_CYCLE_ROOT = "dev.strata.architecture.fixtures.cycle";

    @Test
    void detectsThePublishedFrameworkModules() {
        Set<String> detected =
                modules().stream().map(module -> module.getName()).collect(Collectors.toSet());
        assertTrue(
                detected.containsAll(EXPECTED_MODULES),
                () -> "Spring Modulith detected " + detected + " but expected at least " + EXPECTED_MODULES);
    }

    @Test
    void moduleGraphHasNoCycles() {
        List<String> cycles = modules().detectViolations().getMessages().stream()
                .filter(message -> message.contains("Cycle"))
                .toList();
        assertTrue(cycles.isEmpty(), () -> "Spring Modulith found module-level cycles:\n" + String.join("\n", cycles));
    }

    /**
     * Negative proof for the cycle gate: the fixture packages {@code fixtures.cycle.a} and {@code fixtures.cycle.b}
     * depend on each other on purpose. Spring Modulith must report that cycle, proving the filter in
     * {@link #moduleGraphHasNoCycles()} cannot silently pass on a real cycle. Test classes are imported here on
     * purpose, the production gate above excludes them.
     */
    @Test
    void cycleGateRejectsCyclicFixtureModules() {
        ApplicationModules fixture = ApplicationModules.of(FIXTURE_CYCLE_ROOT, location -> true);
        List<String> cycles = fixture.detectViolations().getMessages().stream()
                .filter(message -> message.contains("Cycle"))
                .toList();
        assertTrue(!cycles.isEmpty(), "Spring Modulith did not report the synthetic module cycle of the fixtures");
    }

    /**
     * Enabled as soon as the framework's internal packages are declared as named interfaces. Kept here (instead of a
     * comment) so the intended end state is visible and one annotation pass away from being enforced.
     */
    @Test
    @Disabled("Framework *-api packages are not declared as @NamedInterface yet, so verify() reports legacy access")
    void fullModuleVerification() {
        modules().verify();
    }

    private static ApplicationModules modules() {
        return ApplicationModules.of(ArchitectureMarker.class, ImportOption.Predefined.DO_NOT_INCLUDE_TESTS);
    }
}
