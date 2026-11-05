# Strata Admin Framework

[![CI](https://github.com/simplezac1995/strata/actions/workflows/ci.yml/badge.svg)](https://github.com/simplezac1995/strata/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-blue.svg)](https://openjdk.org/projects/jdk/25/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.simplezac1995/strata-spring-boot-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.simplezac1995/strata-spring-boot-starter)

A modular-monolith backend framework for admin/casino-style products: RBAC, operations, notifications, AI tooling
and a plugin SPI, with module boundaries enforced by Maven, Spring Modulith and ArchUnit.

This repository is the **open-source framework core**. The deployable product (the app that wires the framework
together with business domains) lives in a separate private repository; everything here is reusable as libraries.

## Requirements

- Java 25, Maven 3.9+
- PostgreSQL 18 (tests + runtime; see `docs/`)
- Redis 7 (rate limiting, locks, distributed coordination)

## Modules

| Module | Responsibility |
| :--- | :--- |
| `strata-common/*` | core (Result/Problem Details, exceptions), security (JWT, data scope, audit), data (JPA base, flyway helpers) |
| `strata-system/*` | RBAC, configuration, notifications, operations, compliance, statistics |
| `strata-ai/*` | chat, knowledge base, model/provider configuration |
| `strata-plugin/*` | plugin SPI plus reference plugins (storage, Stripe, OpenAI, Excel, AES, Flowable) |
| `strata-generator` | code generator (preview / ZIP / write) |
| `strata-starter/*` | Spring Boot starter that wires the framework |
| `examples/strata-module-example` | the golden path for adding a feature module (copy it to start) |
| `strata-ui/` | admin console (React 19 + MUI 9): RBAC, system, AI, plugins, ops and settings pages; business pages stay private |

## Build

```bash
cd strata
mvn -B verify          # compile + tests + Spotless + ArchUnit + JaCoCo
mvn -B -DskipTests install

cd ../strata-ui
pnpm install
pnpm check             # format + lint + type-check + tests + production build
```

## Getting started

```xml
<dependency>
    <groupId>io.github.simplezac1995</groupId>
    <artifactId>strata-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

2. Write an ordinary Spring Boot application. The starter scans the system domain, registers its entities and
   repositories and wires the security components, so no scanBasePackages is required:

    @SpringBootApplication
    public class MyAdminApplication {
        public static void main(String[] args) {
            SpringApplication.run(MyAdminApplication.class, args);
        }
    }

0.1.0 is published on Maven Central, so no extra repository configuration is required.

3. Point `spring.datasource.*` at PostgreSQL 18. The starter ships the framework schema as a Flyway migration
   (`classpath:db/strata-framework`); keep that location and append your own, for example
   `spring.flyway.locations=classpath:db/strata-framework,classpath:db/migration`.
   To see a working setup immediately, run `examples/strata-minimal-app` with `mvn spring-boot:run`.
4. Required configuration: strata.security.jwt.secret (at least 32 characters) and
   strata.seed.admin-password (at least 16 characters in production).
5. Swagger UI is at `/swagger-ui.html`. Optional modules auto-configure themselves when their jar is on the
   classpath: `strata-ai-biz` (plus an LLM plugin such as `strata-plugin-llm-openai`) and the code generator
   (`strata-generator`, enabled with `strata.generator.enabled=true`).

Read the structure conventions (docs/17) for the api/biz/web layering and examples/strata-module-example for a
complete auto-configured module whose tests need no database.

## Container image

A single image builds and runs the minimal application (Java 25 + PostgreSQL):

    docker build -t strata-minimal .
    docker run --rm -p 8080:8080 -e STRATA_DB_HOST=host.docker.internal \
      -e STRATA_JWT_SECRET=change-me-to-a-long-random-string-of-at-least-32-chars \
      -e STRATA_ADMIN_PASSWORD=change-me-16-chars strata-minimal

## Repository policy

- Apache-2.0, SPDX identifier on every source file (`node scripts/verify-spdx.mjs` in CI).
- 500-line file cap (`node scripts/verify-file-size.mjs`).
- Architecture rules in `ArchitectureRulesTest`: controllers live in `web`, `api` is the only cross-module surface.
- Generated configuration reference: `docs/配置参考.md` (CI checks it is up to date).

## Contributing

See `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md` and `SECURITY.md`.

## License

Apache License 2.0 — see `LICENSE`.
