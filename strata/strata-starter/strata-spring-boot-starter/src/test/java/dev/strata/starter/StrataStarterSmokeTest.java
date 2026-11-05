// SPDX-License-Identifier: Apache-2.0
package dev.strata.starter;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.common.security.jwt.JwtTokenService;
import dev.strata.system.rbac.biz.web.AuthController;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The starter contract, proven by a consumer that only depends on the starter: a bare {@code @EnableAutoConfiguration}
 * application starts, the system domain is wired (RBAC controller, JWT service, repositories/entities) and the schema
 * is created from the framework entities.
 *
 * <p>Flyway is disabled on purpose: the open-source repository does not ship the product's migration set, so the
 * starter is verified against Hibernate ddl-auto in an isolated schema. A consumer supplies its own migrations.
 */
@SpringBootTest(
        classes = StrataStarterSmokeTest.ConsumerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.flyway.enabled=false",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.default_schema=starter_smoke",
            "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
            "strata.demo-data.enabled=false",
            "strata.security.jwt.secret=${STRATA_JWT_SECRET:strata-starter-smoke-secret-at-least-32-characters}",
            "strata.system.seed.admin-password=${STRATA_ADMIN_PASSWORD:starter-smoke-admin-password}",
            "spring.main.banner-mode=off"
        })
class StrataStarterSmokeTest {

    /** Exactly what a consumer writes: no component scan, no entity/repository scan. */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class ConsumerApplication {}

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext context;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", StrataStarterSmokeTest::databaseUrl);
        registry.add("spring.datasource.username", StrataStarterSmokeTest::databaseUser);
        registry.add("spring.datasource.password", StrataStarterSmokeTest::databasePassword);
    }

    private static String databaseUrl() {
        Map<String, String> env = System.getenv();
        String url = env.get("SPRING_DATASOURCE_URL");
        if (url != null && !url.isBlank()) return url;
        return "jdbc:postgresql://%s:%s/%s"
                .formatted(
                        env.getOrDefault("STRATA_DB_HOST", "127.0.0.1"),
                        env.getOrDefault("STRATA_DB_PORT", "5432"),
                        env.getOrDefault("STRATA_DB_NAME", "strata"));
    }

    private static String databaseUser() {
        Map<String, String> env = System.getenv();
        return env.getOrDefault("SPRING_DATASOURCE_USERNAME", env.getOrDefault("STRATA_DB_USER", "strata"));
    }

    private static String databasePassword() {
        Map<String, String> env = System.getenv();
        return env.getOrDefault("SPRING_DATASOURCE_PASSWORD", env.getOrDefault("STRATA_DB_PASSWORD", "strata"));
    }

    @AfterAll
    static void dropSmokeSchema() {
        try (Connection connection = DriverManager.getConnection(databaseUrl(), databaseUser(), databasePassword());
                Statement statement = connection.createStatement()) {
            statement.execute("drop schema if exists starter_smoke cascade");
        } catch (Exception ignored) {
            // the embedded context already dropped its tables; a failed cleanup must not fail the build
        }
    }

    @Test
    void consumer_application_starts_with_only_the_starter_on_the_classpath() {
        assertThat(port).isPositive();
        assertThat(context.getBean(JwtTokenService.class)).isNotNull();
        assertThat(context.getBean(AuthController.class)).isNotNull();
        assertThat(context.containsBean("userRepository")).isTrue();
    }
}
