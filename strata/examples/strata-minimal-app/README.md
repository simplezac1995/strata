# strata-minimal-app

The smallest honest consumer of the framework: one `@SpringBootApplication`, the starter dependency, and the
optional AI modules. Nothing else is scanned by hand.

## What it proves

| Promise | Where |
| :--- | :--- |
| One dependency wires the framework | `pom.xml` only depends on `strata-spring-boot-starter` |
| No component/entity/repository scan in the host | `MinimalApplication` has a plain `@SpringBootApplication` |
| The framework schema ships with the framework | starter resource `db/strata-framework/V1__strata_framework.sql`, applied by Flyway |
| Swagger UI comes for free | `StrataOpenApiConfiguration` in the starter (`/swagger-ui.html`) |
| Optional modules are optional | `strata-ai-biz` is an explicit dependency and auto-configures itself |

## Run it

```bash
# PostgreSQL 18 (Redis is only needed for distributed rate limiting/locks; defaults work without it)
export STRATA_DB_HOST=127.0.0.1 STRATA_DB_PORT=5432 STRATA_DB_NAME=strata STRATA_DB_USER=strata STRATA_DB_PASSWORD=strata
export STRATA_JWT_SECRET=change-me-to-a-long-random-string-of-at-least-32-chars
export STRATA_ADMIN_PASSWORD=change-me-16-chars

cd examples/strata-minimal-app
mvn spring-boot:run
```

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- API base: <http://localhost:8080/api/v1>
- Default login user is seeded by `strata.seed.admin-password`; RBAC menus/roles are part of the framework migrations.

## Tests

```bash
mvn -pl examples/strata-minimal-app -am test
```

`MinimalApplicationSmokeTest` applies the framework migration to an isolated PostgreSQL schema and lets Hibernate
validate the mapping against it (needs a reachable PostgreSQL, same as the backend tests).
`FrameworkSchemaExportTest` regenerates `framework-schema.sql` from the JPA mapping — copy it over the starter's
migration file when an entity changes (see the test Javadoc for the exact command).
