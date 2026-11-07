# Strata

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![CI](https://github.com/simplezac1995/strata/actions/workflows/ci.yml/badge.svg)](https://github.com/simplezac1995/strata/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.simplezac1995/strata-spring-boot-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.simplezac1995/strata-spring-boot-starter)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

A modular-monolith backend framework for admin products: RBAC, operations, notifications, AI tooling and a plugin
SPI, with module boundaries enforced by Maven, Spring Modulith and ArchUnit.

Built on Spring Boot 4.1 and **JDK 25** (build with JDK 25, run on Java 21+ �?see [Build requirement](#build-requirement)). Ships the backend
framework, a React admin console and runnable examples.

## Try the demo

**Hosted demo:** <https://demo.simplecharlie.dev> �?sign in as `admin` / `admin123` (shared evaluation instance: data resets
periodically and is not private). Prefer a local instance for anything beyond a quick look:

```bash
git clone https://github.com/simplezac1995/strata.git
cd strata
docker compose up --build
```

- Admin console: <http://localhost:3001> (login `admin` / `demo-admin-password`)
- API documentation: <http://localhost:8080/swagger-ui.html>

Local evaluation only �?replace every demo secret before deploying. The optional Keycloak SSO overlay:

```bash
docker compose -f docker-compose.yml -f docker-compose.sso.yml up --build
```

Then click **Continue with Keycloak** on the login page (`demo` / `demo123`). The demo user is mapped to the
`admin` role through its Keycloak group:

![SSO demo](./docs/screenshots/sso-demo.gif)

## Screenshots

| Login | Dashboard |
| :---: | :---: |
| ![Login](./docs/screenshots/login.png) | ![Dashboard](./docs/screenshots/dashboard.png) |

| User management (RBAC) | AI chat |
| :---: | :---: |
| ![User management](./docs/screenshots/users.png) | ![AI chat](./docs/screenshots/ai-chat.png) |

| SSO login (Keycloak) | After SSO sign-in |
| :---: | :---: |
| ![SSO login](./docs/screenshots/sso-login.png) | ![SSO dashboard](./docs/screenshots/sso-dashboard.png) |
| SAML login (Keycloak) | SAML console |
| ![SAML login](./docs/screenshots/saml-login.png) | ![SAML dashboard](./docs/screenshots/saml-dashboard.png) |

Screenshots taken from the repository's own `docker compose` demo.

## Features

- **One dependency to start**: the starter auto-configures the system domain (RBAC, users, roles, menus, operations,
  configuration), its entities and repositories, security components, scheduling, Swagger UI and the database schema
  as a Flyway migration.
- **Explicit module boundaries**: Maven modules plus Spring Modulith and ArchUnit verification; cross-domain calls go
  through `*-api` contracts only.
- **Optional capabilities as modules or plugins**: AI (models, chat, knowledge bases), storage, payments, LLM gateways,
  Excel, field encryption and workflows are enabled by adding a dependency and a property, never by changing scans.
- **Host integration points**: `VerificationSender`, `BackupExecutor` and `AuditChangeApplier` ship safe defaults that
  a deployment replaces with its own beans.
- **Production plumbing included**: JWT/RBAC menu permissions, RFC 7807 errors, trace ids, rate limiting, idempotency,
  auditing, Flyway, CycloneDX SBOM, JaCoCo floors and a Dockerfile for the minimal application.
- **SCIM 2.0 provisioning**: Okta / Entra ID / OneLogin can create, update, suspend users and sync groups �?RBAC roles through a token-guarded SCIM endpoint with ETag/If-Match, complex filters and bulk operations.
- **SSO / OIDC login**: authorization code + PKCE exchanged server-side, provider userinfo mapped to the normal JWT/RBAC session; provider groups map to RBAC roles (auto-provision + optional per-login sync).
- **SAML 2.0 login**: SP-initiated SAML with XML-DSig verification, metadata-driven IdP trust, email-based user mapping/auto-provision and the same JWT/RBAC session; no OpenSAML dependency.
- **Admin console**: React 19 + MUI 9, RBAC-driven navigation, lazy routes, i18n, accessibility tests.

## What is in this repository (editions)

Strata is open core, with a boundary you can verify from this repository:

- **Community �?this repository (Apache-2.0)**: the framework modules, the starter, the plugin SPIs, the runnable
  examples and the generic React admin console.
- **Pro / Enterprise �?not in this repository**: business-vertical modules for regulated products. They consume the
  published framework from Maven Central; nothing in this repository depends on them.
- **No SaaS lock-in**: everything published here is Apache-2.0 and runs on your own infrastructure. The private
  modules are a separate, optional product.

**Case study**: the framework is used in production for a real high-stakes vertical (payments, KYC and
responsible-gambling compliance), with its business modules kept private. This describes the maintainer's own
deployment only �?no external customers are claimed.

## Repository layout

| Path | Content |
| :--- | :--- |
| `strata/` | Maven reactor: framework modules, starter, plugins, generator, examples |
| `strata-ui/` | React admin console |
| `docs/` | architecture, conventions, adoption guide and generated configuration reference |
| `strata/examples/` | minimal runnable application and the module golden path |
| `scripts/` | repository policy gates and the configuration-reference generator |

## Who maintains this

Strata is currently maintained by **[@simplezac1995](https://github.com/simplezac1995)** as a single maintainer.

- **Priorities and plans**: [ROADMAP.md](./ROADMAP.md); ownership and bus factor: [MAINTAINERS.md](./MAINTAINERS.md).
- **Issues and questions**: [GitHub Issues](https://github.com/simplezac1995/strata/issues) �?bug reports, feature
  requests and usage questions all land there.
- **Security**: never open a public issue; follow [SECURITY.md](./SECURITY.md).
- **Response expectations**: this is maintainer-funded work, so issues and pull requests are triaged on a best-effort
  basis �?expect days, not hours. Security reports follow the targets in SECURITY.md. Small, tested, focused pull
  requests are the easiest to review.

## Build requirement

**Build with JDK 25; run on Java 21+.** The project compiles with a JDK 25 toolchain targeting the Java 21 bytecode
level (`maven.compiler.release=21`, no preview features), and CI starts the built application on a Java 21 runtime.
Maven 3.9+ is needed for the backend; Node 22 and pnpm 11 for the admin console. Publishing stays on a single JDK 25
build matrix, so there is no second toolchain to maintain.

## Quick start

```bash
git clone https://github.com/simplezac1995/strata.git
cd strata/examples/strata-minimal-app
mvn spring-boot:run    # JDK 25 build (Java 21 bytecode); PostgreSQL 18 via STRATA_DB_* variables; Swagger at /swagger-ui.html
```

Or add the published starter to an existing application:

```xml
<dependency>
  <groupId>io.github.simplezac1995</groupId>
  <artifactId>strata-spring-boot-starter</artifactId>
  <version>0.11.0</version>
</dependency>
```

The starter ships the framework schema at `classpath:db/strata-framework` (`V1__strata_framework.sql` and
`V2__idempotency_record.sql`); keep that Flyway location and append your own migrations from `V3` upward. AI and
generator modules auto-configure themselves when their dependencies are present.

## Documentation

- [Getting started](./docs/tutorials/getting-started.md)
- [Add a module](./docs/tutorials/add-a-module.md)
- [AI integration](./docs/tutorials/ai-integration.md)
- [How Strata compares](./docs/comparison.md)

- [Architecture](./docs/architecture.md)
- [Engineering conventions](./docs/conventions.md)
- [Adoption guide](./docs/adoption.md)
- [SSO / OIDC](./docs/sso-oidc.md)
- [SAML 2.0](./docs/saml.md)
- [Configuration reference](./docs/configuration-reference.md)
- [Roadmap](./ROADMAP.md) · [Maintainers](./MAINTAINERS.md)
- [Contributing](./CONTRIBUTING.md) · [Security](./SECURITY.md) · [Changelog](./CHANGELOG.md)

## Build and test

```bash
cd strata
mvn -B verify                      # JDK 25 build (Java 21 bytecode): compile + tests + Spotless + ArchUnit + JaCoCo

cd ../strata-ui
pnpm install --frozen-lockfile
pnpm check                         # typecheck + lint + tests + i18n + formatting + build + bundle size
```

The exported repository is a single curated history of the framework; the private product repository that consumes it
is not part of this project.

## Versioning and releases

Semantic versioning with immutable Maven Central releases: pushing a `v*` tag runs the release workflow (GPG signing,
SBOM, Central Portal publish). See [CHANGELOG.md](./CHANGELOG.md).

## License

Apache License 2.0 �?see [LICENSE](./LICENSE) and [NOTICE](./NOTICE).
