// SPDX-License-Identifier: Apache-2.0
package dev.strata.example.minimal;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.system.rbac.biz.web.AuthController;
import io.swagger.v3.oas.models.OpenAPI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The open-source promise, verified against a throw-away database: the starter framework migration creates the
 * schema, Hibernate validates the JPA mapping against it, the system domain and the optional AI modules are wired,
 * and Swagger is configured.
 *
 * <p>The database is created (and dropped) by the test so it neither depends on nor pollutes a developer/CI database.
 */
@SpringBootTest(
        classes = MinimalApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/strata-framework",
            "spring.jpa.hibernate.ddl-auto=validate",
            "strata.seed.enabled=false",
            "strata.demo-data.enabled=false",
            "strata.security.jwt.secret=strata-minimal-smoke-secret-at-least-32-characters",
            "strata.system.seed.admin-password=minimal-smoke-admin-password",
            "spring.main.banner-mode=off"
        })
class MinimalApplicationSmokeTest {

    private static final String DATABASE = "strata_minimal_smoke";
    private static boolean databasePrepared;

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext context;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        prepareDatabase();
        registry.add("spring.datasource.url", MinimalApplicationSmokeTest::databaseUrl);
        registry.add("spring.datasource.username", MinimalApplicationSmokeTest::databaseUser);
        registry.add("spring.datasource.password", MinimalApplicationSmokeTest::databasePassword);
    }

    private static Map<String, String> env() {
        return System.getenv();
    }

    private static String baseUrl() {
        String url = env().get("SPRING_DATASOURCE_URL");
        if (url != null && !url.isBlank()) return url.split("\\?")[0];
        return "jdbc:postgresql://%s:%s/%s"
                .formatted(
                        env().getOrDefault("STRATA_DB_HOST", "127.0.0.1"),
                        env().getOrDefault("STRATA_DB_PORT", "5432"),
                        env().getOrDefault("STRATA_DB_NAME", "strata"));
    }

    private static String databaseUrl() {
        return baseUrl().replaceAll("/[^/]+$", "/" + DATABASE);
    }

    private static String adminUrl() {
        return baseUrl().replaceAll("/[^/]+$", "/postgres");
    }

    private static String databaseUser() {
        return env().getOrDefault("SPRING_DATASOURCE_USERNAME", env().getOrDefault("STRATA_DB_USER", "strata"));
    }

    private static String databasePassword() {
        return env().getOrDefault("SPRING_DATASOURCE_PASSWORD", env().getOrDefault("STRATA_DB_PASSWORD", "strata"));
    }

    private static synchronized void prepareDatabase() {
        if (databasePrepared) return;
        try (Connection connection = DriverManager.getConnection(adminUrl(), databaseUser(), databasePassword());
                Statement statement = connection.createStatement()) {
            statement.execute("drop database if exists " + DATABASE + " with (force)");
            statement.execute("create database " + DATABASE);
        } catch (Exception exception) {
            throw new IllegalStateException("could not prepare the smoke-test database", exception);
        }
        databasePrepared = true;
    }

    @AfterAll
    static void dropDatabase() {
        try (Connection connection = DriverManager.getConnection(adminUrl(), databaseUser(), databasePassword());
                Statement statement = connection.createStatement()) {
            statement.execute("drop database if exists " + DATABASE + " with (force)");
        } catch (Exception ignored) {
            // a failed cleanup must not fail the build
        }
    }

    @Test
    void applies_the_framework_migration_and_wires_starter_plus_ai_modules() {
        assertThat(port).isPositive();
        assertThat(context.getBean(AuthController.class)).isNotNull();
        assertThat(context.getBean(OpenAPI.class)).isNotNull();
        // the AI add-on auto-configured itself from its jar
        assertThat(context.getBeanNamesForType(dev.strata.ai.biz.service.AiLinkageRuleAppService.class).length)
                .isPositive();
    }
}
