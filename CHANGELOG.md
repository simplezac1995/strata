# Changelog

This project follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
[Semantic Versioning](https://semver.org/).

**Version policy**: every deployment-observable change counts as a version change — a database migration, a
configuration key, an endpoint or an error code. Migrations are immutable (a released migration is never
edited; fixes add new files), so migration lists and application versions can be mapped in support
tickets.

This file describes the **open-source framework** repository. The deployable business product lives in a
separate private repository and has its own release notes.

## [Unreleased]

Nothing yet.

## [0.2.0] - 2026-11-05

### Changed

- Release automation is now end-to-end: the release workflow imports the GPG key, signs and uploads the SBOM, and
  `autoPublish=true` lets a validated deployment go public without a manual Central Portal step. Manual
  `workflow_dispatch` dry-runs (build + sign + SBOM, no deploy) work too.
- Version coordinates are now rendered from `VERSION` everywhere the open-source export mentions them (README,
  local release script), so a version bump cannot leave stale coordinates behind.

### Fixed

- `central-publishing-maven-plugin` bumped from 0.7.0 to 0.12.0: 0.7.0 mis-parsed the Central Portal warnings
  payload while waiting for validation and reported a false failure after a successful upload.
- Tag releases no longer fail on a fresh runner with `No secret key`: the workflow imports `GPG_PRIVATE_KEY` and
  passes the passphrase to the GPG plugin.
- Manual release dry-runs no longer fail the tag/version check (it only applies to tag pushes).


## [0.1.0] - 2026-11-05

### Added

- **Runnable minimal application** (`examples/strata-minimal-app`): a starter-only `@SpringBootApplication` that
  starts against PostgreSQL, applies the framework schema with Flyway, exposes Swagger UI and auto-configures the
  optional AI modules; its smoke test runs the whole chain against a throw-away database.
- **Framework schema migration ships with the starter** (`db/strata-framework/V1__strata_framework.sql`, generated
  from the JPA mapping and validated by Hibernate `validate` in CI).
- **AI and generator auto-configuration**: adding `strata-ai-biz` (plus an LLM plugin) or enabling
  `strata-generator` is now enough for those modules to wire themselves; both share the
  `strata.starter.auto-configuration` switch.
- **Swagger/OpenAPI for consumers**: the starter configures the OpenAPI document and grouped views
  (`/swagger-ui.html`).
- **Golden-path example module** (`examples/strata-module-example`): api/service/web split, Spring Boot
  auto-configuration discovered through `AutoConfiguration.imports`, an opt-out property, typed
  not-found errors, and unit tests that need neither a database nor a Spring context.
- **Real starter auto-configuration**: `strata-spring-boot-starter` now scans the system domain, registers
  its entities and repositories, enables scheduling and brings the security components (audit, crypto,
  rate-limit aspect, signature helper) that were previously only wired by the application's own component
  scan. A bare `@SpringBootApplication` consumer is covered by a smoke test.
- **Open-source repository tooling**: `scripts/export-oss.mjs` produces a framework-only tree (business
  modules, deployable app, business UI pages and private documents excluded, admin UI filtered to the
  generic console, business i18n dictionaries pruned), and `scripts/publish-oss.mjs` validates and
  publishes it with hard gates (single clean commit, no business paths, required files, remote only
  `main`, no stale pull-request refs).
- **Release pipeline**: a `release` Maven profile with enforcer rules, CycloneDX SBOM, sources, javadoc,
  GPG signing and Central Portal publishing; `.github/workflows/release.yml` publishes on `v*` tags.
- **Generated configuration reference** (`docs/配置参考.md`) built from the configuration metadata the
  build produces, with a CI drift check.
- **SPDX on every source file** (Java, TypeScript, scripts) and a repository-wide file-size gate.
- **English governance documents** and a dedicated English README for the open-source repository.

### Changed

- All 154 controllers moved into `web` packages (one ArchUnit rule locks the convention); the older
  `biz.controller` package is retired.
- All 472 `@PreAuthorize` literals replaced by 264 `Permissions` constants (one authority vocabulary).
- Domain events are published through Spring's `ApplicationEventPublisher` directly; hashing uses
  commons-codec; the custom tracing/XSS/WebSocket modules, API-versioning, cache-statistics and
  resilience configuration classes that nothing used were deleted.
- Web integration tests share one `WebTestSupport` scaffold instead of 27 MockMvc setups and 18 private
  login helpers.
- The admin console is split before publication: the open-source tree keeps the generic console, business
  pages are removed by the export script.

### Fixed

- Seed-password configuration prefix in the minimal application (`strata.system.seed.admin-password`); a real HTTP
  login test now proves the seeded admin, JWT issuance and the protected-endpoint chain end to end.
- The starter consumer no longer fails without a Micrometer registry (`BusinessMetrics` falls back to a
  simple registry) or without the application's component scan (`FieldEncryptor`, audit and rate-limit
  aspects are now registered).
- `Menu`'s composite index referenced a non-existent `sort` column; it now points at `sort_order`.
- `updateRecovery` frontend test was committed without its imports and broke `tsc`.
- The browser smoke test no longer treats expected offline-reload network errors as failures.

## Earlier history

The framework was developed inside the product repository before the open-source split. Architectural
decisions and their rationale live in that repository's ADR set; they are not exported here.
