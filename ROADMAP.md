# Roadmap

> This is a plan, not a promise: priorities can change when users or contributors surface a better need. The only
> committed boundary is the platform baseline below.

## Platform baseline (committed)

- **Build with JDK 25; run on Java 21+.** JDK 25 is the single supported build toolchain. Published artifacts target
  Java 21 bytecode and the public CI starts the built application on a Java 21 runtime on every push; there is no
  Java 17 (or older) support line.
- PostgreSQL 18 and Redis are the only supported infrastructure dependencies.

## 0.11 — make the framework auditable

- **Verifiable boundary gates in this repository**: architecture rules (ArchUnit + Spring Modulith) that run in the
  public CI, so "enforced module boundaries" is demonstrated here rather than merely asserted.
- **Public demo and documentation site**: a hosted demo of the admin console plus a documentation site generated from
  `docs/`, so evaluators can try the product before cloning.
- **English-first test naming and comments**: remove the remaining Chinese identifiers and comments from the
  framework sources and tests.
- **Delivered during 0.11.x**: hosted demo + documentation site, public architecture tests, TOTP MFA documentation,
  reference deployment/observability assets and a scheduled Java SBOM gate.

## 1.0 — stable platform

Exit criteria (all required):

- **API stability**: the published `*-api` contracts and the starter are frozen for a 1.x line; breaking changes
  follow semver and a deprecation window.
- **External adopters**: at least three teams outside the maintainer's own deployment have shipped something on the
  published framework and confirmed it. Trials count; no customer relationship is implied.
- **Governance**: `MAINTAINERS.md` / `CODEOWNERS` current, the security policy exercised, and a second maintainer or a
  documented continuity plan in place.

## Decisions (0.x)

- **TOTP MFA ships** as a per-user opt-in second factor; recovery is an audited administrator reset (no backup codes
  in 0.x).
- **The admin console is a reference application**, not an npm library: integrate through the REST API or fork the
  console.
- **No Spring Initializr/Maven archetype integration in 0.x**: bootstrap from the minimal examples + BOM; an Initializr
  contribution is evaluated for 1.0.

## Non-goals

- Microservices orchestration, service mesh or Kubernetes as a first-class deployment target — Strata is a modular
  monolith and Docker Compose is the supported default deployment.
- A Java 17 (or older) support line; Java 21+ runtime is supported.
- SaaS hosting of the admin product.
