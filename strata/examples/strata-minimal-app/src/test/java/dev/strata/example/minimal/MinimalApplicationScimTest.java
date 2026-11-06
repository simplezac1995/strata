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
 * End-to-end proof of the SCIM wiring: the machine-to-machine token is enforced, users can be created/patched/deleted and
 * a group carries role members — all against the real starter schema and security chain.
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
            "strata.scim.enabled=true",
            "strata.scim.token=minimal-scim-test-token",
            "strata.security.jwt.secret=strata-minimal-scim-secret-at-least-32-characters",
            "strata.system.seed.admin-password=minimal-scim-admin-password",
            "spring.main.banner-mode=off"
        })
class MinimalApplicationScimTest {

    private static final String DATABASE = "strata_minimal_scim";
    private static final String TOKEN = "minimal-scim-test-token";
    private static boolean databasePrepared;

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        prepareDatabase();
        registry.add("spring.datasource.url", MinimalApplicationScimTest::databaseUrl);
        registry.add("spring.datasource.username", MinimalApplicationScimTest::databaseUser);
        registry.add("spring.datasource.password", MinimalApplicationScimTest::databasePassword);
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
            throw new IllegalStateException("could not prepare the SCIM-test database", exception);
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

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return http.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .header("Authorization", "Bearer " + TOKEN)
                        .header("Content-Type", "application/scim+json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> patch(String path, String body) throws Exception {
        return http.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .header("Authorization", "Bearer " + TOKEN)
                        .header("Content-Type", "application/scim+json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Test
    void scim_requires_token_and_supports_user_and_group_lifecycle() throws Exception {
        // fail closed without the bearer token
        assertThat(get("/scim/v2/Users", null).statusCode()).isEqualTo(401);
        assertThat(get("/scim/v2/ServiceProviderConfig", TOKEN).statusCode()).isEqualTo(200);
        assertThat(get("/scim/v2/Users", TOKEN).body()).contains("ListResponse");

        // create a provisioned user
        String createUser =
                """
                {"schemas":["urn:ietf:params:scim:schemas:core:2.0:User"],"userName":"scim-user",
                 "name":{"formatted":"SCIM User"},"emails":[{"value":"scim@example.com","primary":true}],
                 "active":true}
                """;
        HttpResponse<String> created = post("/scim/v2/Users", createUser);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("scim-user");
        String userId = created.body().replaceAll(".*\"id\":\"(\\d+)\".*", "$1");
        assertThat(userId).matches("\\d+");

        // patch the account inactive
        HttpResponse<String> patched = patch(
                "/scim/v2/Users/" + userId,
                "{\"Operations\":[{\"op\":\"replace\",\"path\":\"active\",\"value\":false}]}");
        assertThat(patched.statusCode()).isEqualTo(200);
        assertThat(patched.body()).contains("\"active\":false");

        // create a group with the user as member and filter it
        HttpResponse<String> group = post(
                "/scim/v2/Groups", "{\"displayName\":\"SCIM Admins\",\"members\":[{\"value\":\"" + userId + "\"}]}");
        assertThat(group.statusCode()).isEqualTo(201);
        assertThat(group.body()).contains("SCIM Admins");
        assertThat(get("/scim/v2/Groups?filter=displayName%20eq%20%22SCIM%20Admins%22", TOKEN)
                        .body())
                .contains("totalResults\":1");

        // delete the user: 204 then gone
        HttpResponse<String> deleted = http.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/scim/v2/Users/" + userId))
                        .header("Authorization", "Bearer " + TOKEN)
                        .DELETE()
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(deleted.statusCode()).isEqualTo(204);
        assertThat(get("/scim/v2/Users/" + userId, TOKEN).statusCode()).isEqualTo(404);
    }
}
