# Minimal Gradle application

The same minimal application as `examples/strata-minimal-app`, built with **Gradle** and the published
`strata-bom` (no explicit framework versions).

## Prerequisites

- JDK 25 installed (Gradle toolchain compiles to Java 21 bytecode, so you can also run it on Java 21+).
- PostgreSQL 18 and Redis 7 reachable from the application (see the repository demo compose, or run them ad hoc:

```bash
docker run -d --name strata-pg -p 5432:5432 -e POSTGRES_USER=strata -e POSTGRES_PASSWORD=strata -e POSTGRES_DB=strata postgres:18
docker run -d --name strata-redis -p 6379:6379 redis:7-alpine
```)

## Run

```bash
cd examples/strata-minimal-gradle
gradle bootRun        # or: ./gradlew bootRun after generating a wrapper
# open http://localhost:8080/login  (admin / admin123 by default; change it)
curl http://localhost:8080/api/hello
```

## Notes

- `strata-spring-boot-starter` owns the schema (Flyway) and the security filter chain; do not add your own
  `SecurityFilterChain` for the framework paths.
- The BOM version in `build.gradle.kts` must match the published release.
- Kotlin: the starter is a normal Spring Boot dependency; add the Kotlin Gradle plugin and `kotlin-spring` if you want
  Kotlin sources. The community examples are Java-first.
