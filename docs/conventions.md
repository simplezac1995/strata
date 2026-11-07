# Engineering conventions

> These rules are enforced where possible: Spotless, ArchUnit, Spring Modulith, JaCoCo floors and the admin UI gate
> fail the build instead of documenting intent only.

## Backend

### Packages and layering

```
dev.strata.common.*        technical kernel (core, security, data, redisson, ...)
dev.strata.system.*        RBAC, ops, config, communication
dev.strata.ai.*            optional AI domain
dev.strata.plugin.*        plugin SPI implementations
dev.strata.generator       code generator
dev.strata.starter         Spring Boot starter
```

- Root package `dev.strata`; entry point `dev.strata.app.StrataApplication`.
- Directory names and package names always match; controllers live in `...biz.web`.
- Cross-domain calls go through the other module's `*-api` port/DTO — never its tables or `-biz` classes.

### Naming

| Kind | Rule |
| :--- | :--- |
| HTTP entry point | `XxxController` |
| Use-case orchestration | `XxxAppService` |
| Persistence | `XxxRepository` |
| Request / response | `XxxRequest` / `XxxDto` |
| Mapping | `XxxMapper` (MapStruct) |
| Configuration | `XxxConfiguration`, `XxxProperties` |
| Events | `XxxEvent`, `XxxListener` |

### Data and persistence

- Snowflake ids via `IdGenerator`; no database auto-increment/identity columns.
- Audit columns `createdAt/createdBy/updatedAt/updatedBy` with JPA auditing.
- All profiles use `ddl-auto=validate`: Flyway owns the schema.
- Batched inserts/updates enabled; PostgreSQL-only migrations.
- Only Spring Data JPA and parameterised queries; no second ORM and no string-concatenated SQL.

### REST

- `GET` reads, `POST` creates/acts, `PUT` replaces, `DELETE` removes; `PATCH` is not used.
- Server-side pagination; the server clamps `limit`; slow-query threshold is 1000 ms.
- Errors go through `GlobalExceptionHandler` (and the security/limit/idempotency writers) into ProblemDetails.
- Authority strings use the `Permissions` constants, never inline `hasAuthority("...")` literals.

### Logging and configuration

- SLF4J only; never `System.out` or bare `java.util.logging`.
- `TraceIdFilter` writes the MDC trace id and returns `X-Trace-Id`; prod logs are JSON (Loki/ELK friendly).
- Never log credentials or full entities containing secrets.
- New keys use the `strata.*` namespace with defaults and comments; secrets come from environment variables (or
  the AES field-encryption plugin), never from committed YAML.

### Build gates

| Gate | What it enforces |
| :--- | :--- |
| Spotless (Palantir) | formatting, SPDX headers, imports, no trailing whitespace |
| Maven compiler | Java 21 release target (built with the JDK 25 toolchain), parameter names |
| Surefire + JaCoCo | tests plus per-module instruction/branch floors |
| Spring Modulith + ArchUnit | module graph, layering and package rules |
| CycloneDX | SBOM on the release profile |
| Repository scripts | file size ≤ 500 lines, configuration reference up to date, no secrets/private data |

Minimum before pushing: `cd strata && mvn spotless:apply && mvn verify`.

## Admin UI

Stack: React 19, TypeScript 6, MUI 9, Vite 8, TanStack Query 5, Zustand 5, React Router 7, pnpm 11.

- Feature folders under `src/pages/<domain>`; shared building blocks under `src/components/ui` (Button, DataTable,
  Dialog, QueryForm, Pagination, Alert, chart wrappers, ...).
- Page files are PascalCase, route paths kebab-case, hooks `useXxx`.
- Server state belongs to TanStack Query; local UI state to Zustand; no server data cached in Zustand.
- All visible text goes through `t("domain.key")`; `pnpm i18n:verify` and `pnpm i18n:scan` guard the dictionaries.
- Permissions come from the same `module:entity:action` strings as the backend; route guards use `<Perm>`.
- Accessibility: interactive controls have accessible names, axe-core smoke tests cover shared primitives, and the
  component error boundary is always present.
- Every non-entry page is lazy-loaded; `manualChunks` keeps vendor code cached; the single-chunk budget is 600 kB.
- Quality gate: `pnpm check` (typecheck, lint, unused-code scan, unit/component tests, i18n, formatting, production
  build, bundle size). `pnpm exec playwright test --config playwright.minimal.config.ts` runs the E2E smoke test.

Browser support: Chrome/Edge ≥ 90, Firefox ≥ 90, Safari ≥ 14; build target ES2022.
