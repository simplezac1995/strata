# Examples

Two runnable examples live here:

| Example | Purpose |
| :--- | :--- |
| [strata-minimal-app](./strata-minimal-app) | the smallest application: starter dependency, one `@SpringBootApplication`, Flyway schema, Swagger — the reference for a new deployment |
| [strata-module-example](./strata-module-example) | the golden path for adding a feature module: api/biz split, auto-configuration, tests |

## Run the minimal application

```bash
# PostgreSQL 18 must be reachable (see STRATA_DB_* variables)
export STRATA_JWT_SECRET=change-me-to-a-long-random-string-of-at-least-32-chars
export STRATA_ADMIN_PASSWORD=change-me-16-chars
cd strata-minimal-app
mvn spring-boot:run
```

Swagger UI: <http://localhost:8080/swagger-ui.html> · API base: <http://localhost:8080/api/v1>.

## Run the module example tests

```bash
mvn -B -pl examples/strata-module-example -am test
```
