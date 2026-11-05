// SPDX-License-Identifier: Apache-2.0
package dev.strata.example.minimal;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * End-to-end proof of the seed + authentication chain in the open-source setup: the starter's schema migration runs,
 * the RBAC seed creates the admin account, a real HTTP login returns a JWT and the same token is accepted by a
 * protected endpoint while an anonymous request is rejected with 401.
 *
 * <p>Uses its own throw-away database, like the smoke test.
 */
@SpringBootTest(
        classes = MinimalApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/strata-framework",
            "spring.jpa.hibernate.ddl-auto=validate",
            "strata.seed.enabled=true",
            "strata.demo-data.enabled=false",
            "strata.security.jwt.secret=strata-minimal-login-secret-at-least-32-characters",
            "strata.system.seed.admin-password=strata-minimal-admin-password",
            "spring.main.banner-mode=off"
        })
class MinimalApplicationLoginTest {

    private static final String DATABASE = "strata_minimal_login";
    private static final String ADMIN_PASSWORD = "strata-minimal-admin-password";
    private static boolean databasePrepared;

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        prepareDatabase();
        registry.add("spring.datasource.url", MinimalApplicationLoginTest::databaseUrl);
        registry.add("spring.datasource.username", MinimalApplicationLoginTest::databaseUser);
        registry.add("spring.datasource.password", MinimalApplicationLoginTest::databasePassword);
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
            throw new IllegalStateException("could not prepare the login-test database", exception);
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

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> login(String username, String password) throws Exception {
        String body = "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
        return send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/system/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build());
    }

    @Test
    void seeded_admin_can_log_in_and_the_token_opens_a_protected_endpoint() throws Exception {
        HttpResponse<String> response = login("admin", ADMIN_PASSWORD);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("accessToken");
        String token = response.body().replaceAll(".*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        assertThat(token).isNotBlank();

        HttpResponse<String> anonymous =
                send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/system/users"))
                        .GET()
                        .build());
        assertThat(anonymous.statusCode()).isEqualTo(401);

        HttpResponse<String> authorised =
                send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/system/users"))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build());
        assertThat(authorised.statusCode()).isEqualTo(200);
        assertThat(authorised.body()).contains("\"code\":0");
    }

    @Test
    void wrong_password_is_rejected() throws Exception {
        HttpResponse<String> response = login("admin", "definitely-not-the-password");
        assertThat(response.statusCode()).isEqualTo(401);
    }
}
