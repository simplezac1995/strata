// SPDX-License-Identifier: Apache-2.0
package dev.strata.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import dev.strata.architecture.fixtures.ForbiddenController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

/**
 * Architecture gate for the published framework.
 *
 * <p>This is the open-source port of the product repository's architecture test: the rules that describe the reusable
 * framework are kept, the rules that only concerned business domains are omitted. The test lives next to the code it
 * guards, so the exported repository's own {@code mvn verify} fails on a boundary violation instead of relying on a
 * private test the public cannot run.
 *
 * <p>Enforced rules:
 *
 * <ul>
 *   <li>{@code *-biz} modules never depend on plugin implementations, only on {@code strata-plugin-api};
 *   <li>{@code *-biz} modules of different domains never depend on each other;
 *   <li>controllers never depend on JPA entities;
 *   <li>MapStruct mappers live in {@code biz.mapper};
 *   <li>{@code *-api} artifacts stay framework-free and never depend on implementations;
 *   <li>{@code api.dto} types are records;
 *   <li>system/ai sub-domain {@code biz} modules talk through {@code *-api}, not through each other's {@code biz};
 *   <li>framework modules do not depend on the optional domains and do not declare an application entry point;
 *   <li>HTTP controllers live in {@code web} packages.
 * </ul>
 */
class ArchitectureRulesTest {

    /**
     * HTTP entry points live in the module's {@code web} package (see {@code docs/oss/architecture.md}, layering). SCIM
     * 2.0 and SAML 2.0 protocol adapters are incoming-protocol endpoints that currently sit next to their protocol
     * services in {@code biz.scim} / {@code biz.saml}; those two packages are the only documented exceptions.
     */
    private static final ArchRule CONTROLLERS_LIVE_IN_WEB_PACKAGES = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .resideInAnyPackage("..web..", "..scim..", "..saml..")
            .allowEmptyShould(true);

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("dev.strata");
    }

    @Test
    void bizDoesNotDependOnPluginImplementations() {
        noClasses()
                .that()
                .resideInAnyPackage("..system..biz..", "..ai..biz..", "..generator..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..plugin.storage..",
                        "..plugin.stripe..",
                        "..plugin.llm..",
                        "..plugin.excel..",
                        "..plugin.encrypt..",
                        "..plugin.workflow..")
                .check(classes);
    }

    @Test
    void bizModulesDoNotDependOnEachOther() {
        noClasses()
                .that()
                .resideInAnyPackage("..system..biz..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..ai..biz..")
                .check(classes);
        noClasses()
                .that()
                .resideInAnyPackage("..ai..biz..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..system..biz..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void controllersDoNotDependOnEntities() {
        noClasses()
                .that()
                .haveSimpleNameEndingWith("Controller")
                .and()
                .resideInAnyPackage("..system..biz..", "..ai..biz..", "..generator..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..api.entity..")
                .check(classes);
    }

    @Test
    void mappersLiveInBizMapperPackages() {
        // MapStruct mappers stay in biz.mapper; the demo CRUD of system-biz is the documented exception and keeps its
        // mapper next to its resource in biz.demo.
        classes()
                .that()
                .haveSimpleNameEndingWith("Mapper")
                .and()
                .resideInAPackage("dev.strata..")
                .should()
                .resideInAnyPackage("..biz.mapper..", "..biz.demo..")
                .check(classes);
    }

    /**
     * The {@code *-api} artifacts are the published contract surface: other modules, and once released, third parties
     * depend on them. They must stay framework-light, JPA entities and records are the documented content
     * ({@code docs/oss/architecture.md}), Spring Web/JPA plumbing and servlet types are not. Without this lock the first
     * helper placed in an api module silently drags web dependencies into every consumer.
     */
    @Test
    void apiModulesAreFrameworkFree() {
        noClasses()
                .that()
                .resideInAnyPackage("..api..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.servlet..")
                .check(classes);
    }

    /**
     * Api modules describe contracts; implementations depend on them, never the reverse ({@code docs/oss/architecture.md},
     * dependency direction).
     */
    @Test
    void apiModulesDoNotDependOnImplementations() {
        noClasses()
                .that()
                .resideInAnyPackage("..api..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..biz..",
                        "..plugin.storage..",
                        "..plugin.stripe..",
                        "..plugin.llm..",
                        "..plugin.excel..",
                        "..plugin.encrypt..",
                        "..plugin.workflow..")
                .check(classes);
    }

    /**
     * DTOs are records. They are the most-consumed type in the api surface, so a mutable DTO subclass or a
     * getter-class slipping in is a contract change, not a style nit.
     */
    @Test
    void apiDtosAreRecords() {
        classes().that().resideInAPackage("..api.dto..").should().beRecords().check(classes);
    }

    /**
     * System sub-domain biz modules must not depend on the system-biz core. system-biz is the coordination layer that
     * depends on all sub-domains; a reverse dependency creates a cycle and blurs the sub-domain boundary.
     */
    @Test
    void systemSubdomainBizDoesNotDependOnSystemBizCore() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "..system.rbac.biz..",
                        "..system.ops.biz..",
                        "..system.config.biz..",
                        "..system.communication.biz..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..system.biz..")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * System sub-domain biz modules communicate through api modules, not directly through other sub-domain biz modules.
     * The one allowed exception is ops-biz depending on communication-biz (shared community feature configuration).
     */
    @Test
    void systemSubdomainBizPrefersApiOverBizDependencies() {
        noClasses()
                .that()
                .resideInAnyPackage("..system.rbac.biz..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..system.ops.biz..", "..system.config.biz..", "..system.communication.biz..")
                .allowEmptyShould(true)
                .check(classes);
        noClasses()
                .that()
                .resideInAnyPackage("..system.config.biz..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..system.rbac.biz..", "..system.ops.biz..", "..system.communication.biz..")
                .allowEmptyShould(true)
                .check(classes);
        noClasses()
                .that()
                .resideInAnyPackage("..system.communication.biz..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..system.rbac.biz..", "..system.ops.biz..", "..system.config.biz..")
                .allowEmptyShould(true)
                .check(classes);
        noClasses()
                .that()
                .resideInAnyPackage("..system.ops.biz..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..system.rbac.biz..", "..system.config.biz..")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * The AI sub-domain modules use their own api artifacts and never reach into the ai-biz core (the AI counterpart of
     * the system rule above; verified clean when the gate was introduced).
     */
    @Test
    void aiSubdomainBizDoesNotDependOnAiBizCore() {
        noClasses()
                .that()
                .resideInAnyPackage("..ai.chat.biz..", "..ai.config.biz..", "..ai.knowledge.biz..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..ai.biz..")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * The framework layers (common, system, plugin) must not know the optional domains or the development-only
     * generator: dependencies point from the optional modules to the framework, never the other way around.
     */
    @Test
    void frameworkModulesDoNotDependOnDomainModules() {
        noClasses()
                .that()
                .resideInAnyPackage("..common..", "..system..", "..plugin..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..ai..", "..generator..")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Only a host application declares the Spring Boot entry point; the published framework modules must stay
     * importable-by-any-app and therefore must not compete for it.
     */
    @Test
    void frameworkModulesDoNotDeclareAnApplicationEntryPoint() {
        noClasses()
                .that()
                .resideInAnyPackage("..common..", "..system..", "..ai..", "..plugin..", "..generator..", "..starter..")
                .should()
                .beAnnotatedWith(SpringBootApplication.class)
                .check(classes);
    }

    @Test
    void controllersLiveInWebPackages() {
        CONTROLLERS_LIVE_IN_WEB_PACKAGES.check(classes);
    }

    /**
     * Negative proof for the gate itself: the production import skips test classes, so this test imports the misplaced
     * fixture controller ({@link ForbiddenController}) on its own and asserts that the controller rule rejects it. If
     * this test passes, a real controller outside a web package cannot slip through the rule.
     */
    @Test
    void controllerRuleRejectsAControllerOutsideWebPackages() {
        JavaClasses fixture = new ClassFileImporter().importPackagesOf(ForbiddenController.class);
        assertThrows(AssertionError.class, () -> CONTROLLERS_LIVE_IN_WEB_PACKAGES.check(fixture));
    }
}
