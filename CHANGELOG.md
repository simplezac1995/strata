# Changelog

This project follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
[Semantic Versioning](https://semver.org/).

**Version policy**: every deployment-observable change counts as a version change �?a database migration, a
configuration key, an endpoint or an error code. Migrations are immutable (a released migration is never
edited; fixes add new files), so migration lists and application versions can be mapped in support
tickets.

This file describes the **open-source framework** repository. The deployable business product lives in a
separate private repository and has its own release notes.

## [0.10.1] - 2026-11-07

### Added

- A published **architecture gate** (`strata-architecture-tests`): ArchUnit rules for the framework modules and a real
  Spring Modulith module-graph check, so the exported repository's own `mvn verify` fails on a boundary violation
  instead of relying on a private product test. The full `ApplicationModules.verify()` end state is recorded as a
  disabled test until the `*-api` packages are declared as named interfaces.

### Changed

- The open-source documentation is English-first end to end: `docs/configuration-reference.md` replaces the Chinese
  `docs/配置参�?md`, and the tutorial/security/contributing references no longer drift from released artifacts.
- README (EN and zh-CN) now states who maintains the project, the Editions / Open Core boundary and the hard **JDK 25**
  build requirement (single baseline, no Java 21 downgrade line), plus ROADMAP / MAINTAINERS / CODEOWNERS.

### Fixed

- The export pipeline defaults the demo admin console to English, removes the remaining business-only compliance pages
  and internal UI migration scripts from the public tree, renders the version placeholder in every exported document,
  attributes the curated export commit to the maintainer's GitHub identity and trims business dependencyManagement
  coordinates.
- `docs/configuration-reference.md` is regenerated from the current metadata (218 properties / 18 groups).

## [0.11.0] - 2026-11-07

### Added

- **Java 21 runtime compatibility**: the framework is built with a JDK 25 toolchain and now compiles to Java 21
  bytecode (`maven.compiler.release=21`), so applications can run on Java 21+ while the project keeps a single
  JDK 25 build matrix. CI builds the minimal application on JDK 25 and starts it on a Java 21 runtime on every push.
- **Hosted demo**: <https://demo.simplecharlie.dev> runs the exported admin console and API (shared evaluation instance).
- **GitHub Release automation**: a workflow creates the GitHub Release object from the version tag and CHANGELOG, and
  backfills any version tag that is missing a release.

### Changed

- The exported framework no longer ships the private compliance/AML/KYC/geo/exchange/limit backend packages or the
  `CASINO_*` / `COMPLIANCE_*` / `PRO_*` permission vocabulary; those stay in the private product repository.
- Exported tests no longer carry Chinese `@DisplayName` labels; public test names are the English method names.
- The default SQL used by the AI chat tool example no longer references a business table.
- Documentation now states the baseline as "build with JDK 25; run on Java 21+" everywhere (README, contributing
  guide, comparison, roadmap, conventions).

### Fixed

- The OpenAPI document reports the current framework version (`0.11.0`) instead of the stale `0.3.0`.

## [0.11.1] - 2026-11-07

### Added

- **Hosted documentation site** (https://docs.simplecharlie.dev) generated from this repository's `docs/`, linked from the README.
- **FAQ** (`docs/faq.md`): Java baseline, hosted demo, OpenGamma naming clarification, editions and contribution.
- Hosted-demo screenshots in the README, taken from the public evaluation instance.

### Fixed

- GitHub Release automation now extracts the matching CHANGELOG section into each release body (the previous pattern
  never matched, so releases fell back to GitHub's default notes).
- The OpenAPI document reports 0.11.1 instead of a stale version.

## [0.11.2] - 2026-11-07

### Added

- **`strata-bom`**: a consumer BOM managing every published framework artifact, so Maven and Gradle users can drop
  version numbers from framework dependencies.
- **Deployment, operations and security-hardening guides** in public docs: run the image, required secrets, health
  probes, upgrades/rollback, backups, incident runbook and a production security checklist.
- **Plugin development guide** plus a runnable `examples/strata-plugin-example` module (auto-configuration, opt-in
  property, status provider, tests).
- **Code generator tutorial** (endpoints, console flow, generated files, the triple CI gate and what still needs manual
  wiring).
- **Upgrade and support policy** page and a **public ADR digest** (`docs/decisions.md`).
- **Multi-tenancy guide**: community directory-level model and the private/Pro row-level extension point.
- **GOVERNANCE.md** and public docs site entries for all of the above.
- **Supply-chain gates in the public repository**: a Trivy filesystem workflow with dependency review, and a GHCR
  container-image workflow for version tags.
- Releases now attach the CycloneDX `bom.json` asset to the GitHub Release.

### Fixed

- The public docs site is regenerated with the new pages, client-side search and per-page "edit on GitHub" links.
- OpenAPI reports 0.11.2.

## [Unreleased]

Nothing yet.

## [0.10.0] - 2026-11-07

### Added

- **SAML 2.0 single sign-on**: SP-initiated login (HTTP-Redirect AuthnRequest), HTTP-POST Assertion Consumer Service,
  XML-DSig verification with the JDK APIs, metadata-driven IdP trust, audience/recipient/time/InResponseTo validation,
  email-based user mapping and auto-provisioning, one-time code exchange for the normal JWT/RBAC session.
- Admin UI: SAML button on the login page and `/sso-callback?provider=saml` token exchange.
- SSO demo overlay now comes with a Keycloak **SAML** client and an email attribute mapper next to the OIDC client; the
  backend consumes the realm metadata directly (no certificate copy).
- `docs/saml.md` guide, SAML screenshots and a smoke test for the public disabled-by-default config; SAML ACS/exchange
  endpoints are rate limited.
- Optional Redis state store (`strata.saml.state-store=redis`) for RelayState and login codes.

### Notes

- SAML is disabled by default. Supported profile: POST response, signed assertion/response, no SLO, no encrypted
  assertions, no IdP-initiated login.
- No OpenSAML dependency: the framework keeps using only JDK XML-DSig plus its existing stack.

## [0.9.0] - 2026-11-06

### Added

- SCIM **ETag/If-Match** optimistic concurrency: resources expose `meta.version` and single-resource responses carry an
  `ETag` header; stale `If-Match` values are rejected with 412.
- SCIM **complex filters**: `and`/`or`/`not`, parentheses, nested attribute paths (`emails.value`) and the
  `eq ne co sw ew pr gt ge lt le` operators.
- SCIM **bulk operations**: `POST /scim/v2/Bulk` processes up to 100 operations and returns per-operation status and
  resources, so identity providers can sync a page in one request.
- Expanded SCIM guide and an end-to-end test covering the bearer-token gate, create/PATCH with If-Match (including a
  stale 412), groups with members, bulk and delete.

## [0.8.1] - 2026-11-06

### Added

- The demo compose stack enables SCIM with a clearly-marked evaluation token, and the SCIM guide documents a
  copy-paste local walkthrough (ServiceProviderConfig, user creation).
- An end-to-end SCIM wiring test in the minimal app: token required (401 without it), create user, PATCH active,
  create group with members, delete user.

## [0.8.0] - 2026-11-06

### Added

- **SCIM 2.0 provisioning**: token-guarded `/scim/v2` endpoints for Users and Groups (list/filter, create, replace,
  PATCH add/replace/remove, soft-delete, pagination) that let Okta, Microsoft Entra ID or OneLogin provision users and
  sync groups onto framework RBAC roles. Disabled by default; fails closed when no token is configured.
- `GET /scim/v2/ServiceProviderConfig` and a SCIM guide (`docs/scim.md`) with IdP setup and limitations.

## [0.7.0] - 2026-11-06

### Added

- The Keycloak demo now exercises **group �?role mapping** end to end: the realm imports a `strata-admins` group and a
  group-membership mapper, `demo` belongs to that group, and the backend maps the `groups` claim to the `admin`
  role (no default role is configured). A recorded GIF shows the full browser flow in both READMEs.
- `GET /api/v1/system/auth/me` ships again (user + fresh menu tree) for the admin console's session-refresh path;
  the React client had been silently falling back to its cached snapshot on a 404.

### Fixed

- Demo realm/compose overlay: browser-facing authorize endpoint vs container-facing token/userinfo endpoints, group
  claim mapper and PKCE are all configured out of the box.

## [0.6.0] - 2026-11-06

### Added

- **OIDC group/role mapping**: providers can map external groups/roles to framework RBAC roles via
  `roles-claim` (dot paths such as `realm_access.roles`), `role-mappings` and an optional authoritative
  `sync-roles` mode that reconciles an existing user's roles on every login. Auto-provisioned users get the mapped
  roles; `default-role-code` remains the fallback.
- A `Central status` manual workflow (maintainer helper) to query a Central Portal deployment state without
  exposing credentials.

## [0.5.0] - 2026-11-06

### Added

- One-command **Keycloak SSO demo**: `docker compose -f docker-compose.yml -f docker-compose.sso.yml up --build`
  starts Keycloak with an imported realm (`deploy/keycloak/strata-realm.json`), enables the OIDC provider on the
  backend and shows **Continue with Keycloak** on the login page (verified end to end: Keycloak form �?`/sso-callback`
  �?dashboard).
- Redis-backed social-login state store (`strata.social.state-store: redis`): atomic GETDEL + TTL, so the OIDC
  handshake works behind multiple instances; the demo overlay uses it with the bundled Redis service.
- SSO screenshots in both READMEs and the SSO guide.

### Fixed

- Framework schema now ships the `sys_idempotency_record` table as migration `V2` (starters previously failed on
  idempotent POSTs such as the SSO callback with `relation ... does not exist`).
- Public auth pages (login, SSO callback, reset, ...) no longer call `/auth/me` or follow 401 redirects, so the SSO
  callback is not interrupted by the boot-time session probe.
- The Keycloak overlay configures the browser-facing authorize endpoint and the container-facing token/userinfo
  endpoints separately.

## [0.4.0] - 2026-11-06

### Added

- **SSO / OpenID Connect login** for the admin console: authorization code + PKCE, server-side code exchange with the
  client secret, one-time state and redirect-URI whitelist, provider discovery from `issuer-uri`, userinfo identity
  mapping, optional auto-provisioning with a default role, and the normal JWT/RBAC session afterwards. New public
  endpoints: `/api/v1/system/auth/social/config`, `/social/authorize`, `/social/callback`; admin UI shows one SSO
  button per configured provider and completes the flow on `/sso-callback`.
- SSO / OIDC documentation (`docs/sso-oidc.md`), configuration properties reference, and smoke coverage for the public
  login-page config endpoint.

### Changed

- Social login is no longer a handshake skeleton: a configured provider now returns its real authorize URL.
- The demo admin console host port defaults to **3001** (3000 is commonly taken by Grafana).

### Security

- PKCE verifier and client secret stay server-side; state is single-use and expiring; redirect URIs must match the
  configured whitelist exactly; login/callback are rate-limited.

## [0.3.0] - 2026-11-06

### Added

- Framework extension points: `VerificationSender`, `BackupExecutor` and `AuditChangeApplier` with safe default
  beans that deployments override by declaring their own bean.
- Three adoption guides: adding modules/plugins, framework schema & migrations, AI module integration.
- JaCoCo coverage summary in CI (job summary + XML artifact), axe-core accessibility test, and a separate
  `E2E · minimal app + admin console` workflow (manual/weekly).
- `compat` Maven profile running japicmp against the previous release to catch binary incompatibilities.

### Changed

- The open-source export ships six framework documents and only the OSS E2E configuration.

## [0.2.1] - 2026-11-06

### Added

- **Web Vitals ingestion**: `POST /api/v1/metrics/frontend` (unauthenticated like the CSP report endpoint) records
  browser metrics in Micrometer; the admin UI reports to the versioned path in production.
- **Admin API contract test** in `examples/strata-minimal-app`: the paths the admin console calls (system, ops,
  AI knowledge bases, plugins, metrics) and the unauthenticated Web Vitals endpoint are asserted against the real
  handler mapping, so UI/backend path drift fails CI.
- Vendor chunk splitting in the admin UI keeps the single-chunk size gate meaningful after the dependency refresh.

### Changed

- The AI interface reference and the AI chat tool prompt use the real knowledge-base paths
  (`/api/v1/ai/knowledge-bases`).
- The open-source export no longer includes the business support-ticket page/API client, and removes the business
  navigation/breadcrumb labels and their dead i18n keys.

### Security

- npm advisories addressed through pnpm overrides: undici 8.12.2, follow-redirects 1.16.2, brace-expansion 2.1.7,
  ansi-regex 5.0.1, smol-toml 1.9.0, source-map-js 1.2.2; vite pinned to the patched 8.2.4 and vitest 4.1.12.
- Known low-severity advisory `com.github.ulisesbocchio:jasypt-spring-boot-starter` has no upstream fix; it is not
  enabled by default (see SECURITY.md).

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
- **Generated configuration reference** (`docs/配置参�?md`) built from the configuration metadata the
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
