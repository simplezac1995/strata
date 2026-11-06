// SPDX-License-Identifier: Apache-2.0
package dev.strata.example.minimal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Regenerates the framework schema migration that ships with the starter.
 *
 * <p>The source of truth is the JPA mapping, not a hand-written script: this test asks Hibernate to emit the DDL for
 * every entity on the starter + AI classpath, with the database action disabled so no connection is needed. The
 * resulting file is copied to {@code strata-starter/.../db/strata-framework/V1__strata_framework.sql}; regenerate it
 * after changing an entity:
 *
 * <pre>
 * cd strata
 * mvn -o -B -pl examples/strata-minimal-app -am -Dtest=FrameworkSchemaExportTest test
 * cp examples/strata-minimal-app/target/framework-schema.sql \
 *    strata-starter/strata-spring-boot-starter/src/main/resources/db/strata-framework/V1__strata_framework.sql
 * </pre>
 */
@SpringBootTest(
        classes = MinimalApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/strata_schema_export",
            "spring.datasource.username=none",
            "spring.datasource.password=none",
            "spring.jpa.hibernate.ddl-auto=none",
            "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
            "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
            "spring.jpa.properties.jakarta.persistence.schema-generation.database.action=none",
            "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
            "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=target/framework-schema.sql",
            "spring.flyway.enabled=false",
            "strata.seed.enabled=false",
            "strata.demo-data.enabled=false",
            "strata.security.jwt.secret=strata-schema-export-secret-at-least-32-characters",
            "spring.main.banner-mode=off"
        })
class FrameworkSchemaExportTest {

    private static final Path SCHEMA = Path.of("target/framework-schema.sql");

    @BeforeAll
    static void removeStaleSchemaFile() throws Exception {
        // Hibernate appends to an existing script target, so a stale file would duplicate the DDL
        Files.deleteIfExists(SCHEMA);
    }

    @Test
    void hibernate_emits_the_framework_schema_migration() throws Exception {
        assertThat(SCHEMA).exists();
        assertThat(Files.readString(SCHEMA)).containsIgnoringCase("create table");
    }
}
