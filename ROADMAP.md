# Roadmap

> This is a plan, not a promise: priorities can change when users or contributors surface a better need. The only
> committed boundary is the platform baseline below.

## Platform baseline (committed)

- **JDK 25 only.** JDK 25 is the single supported build and runtime baseline. There is no Java 21 downgrade line and
  none is planned: the project follows the latest LTS and keeps one build matrix.
- PostgreSQL 18 and Redis are the only supported infrastructure dependencies.

## 0.11 — make the framework auditable

- **Verifiable boundary gates in this repository**: architecture rules (ArchUnit + Spring Modulith) that run in the
  public CI, so "enforced module boundaries" is demonstrated here rather than merely asserted.
- **Public demo and documentation site**: a hosted demo of the admin console plus a documentation site generated from
  `docs/`, so evaluators can try the product before cloning.
- **English-first test naming and comments**: remove the remaining Chinese identifiers and comments from the
  framework sources and tests.

## 1.0 — stable platform

Exit criteria (all required):

- **API stability**: the published `*-api` contracts and the starter are frozen for a 1.x line; breaking changes
  follow semver and a deprecation window.
- **External adopters**: at least three teams outside the maintainer's own deployment have shipped something on the
  published framework and confirmed it. Trials count; no customer relationship is implied.
- **Governance**: `MAINTAINERS.md` / `CODEOWNERS` current, the security policy exercised, and a second maintainer or a
  documented continuity plan in place.

## Non-goals

- Microservices orchestration, service mesh or Kubernetes as a first-class deployment target — Strata is a modular
  monolith and Docker Compose is the supported default deployment.
- A Java 21 (or older) support line.
- SaaS hosting of the admin product.
