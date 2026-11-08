# Minimal Kotlin application

The minimal application written in **Kotlin** (Gradle Kotlin DSL + the Kotlin Spring plugin), using the published
`strata-bom` so no framework versions are repeated. It is the Kotlin counterpart of
`examples/strata-minimal-app` (Maven/Java) and `examples/strata-minimal-gradle` (Gradle/Java).

## Prerequisites

- JDK 25 installed (Gradle compiles with the Java 21 toolchain, so it runs on Java 21+).
- PostgreSQL 18 and Redis 7 reachable (see `docs/deployment.md` or run them ad hoc):

```bash
docker run -d --name strata-pg -p 5432:5432 -e POSTGRES_USER=strata -e POSTGRES_PASSWORD=strata -e POSTGRES_DB=strata postgres:18
docker run -d --name strata-redis -p 6379:6379 redis:7-alpine
```

## Run

```bash
cd examples/strata-minimal-kotlin
gradle bootRun        # or: ./gradlew bootRun after generating a wrapper
# open http://localhost:8080/login (admin / admin123 by default; change it)
curl http://localhost:8080/api/hello
```

## Notes

- The starter owns the schema (Flyway) and the security filter chain; do not add your own `SecurityFilterChain` for
  framework paths.
- Keep the BOM version in `build.gradle.kts` aligned with the release you deploy.
- Kotlin reflection and the Kotlin Spring plugin are required for Kotlin-based Spring Boot apps; both are ordinary
  Gradle plugins, no framework-side setup.
