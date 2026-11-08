# Architecture decisions (public digest)

> The full ADR set lives in the private product repository; this page summarises the decisions that shape the public
> framework, so consumers can see *why* it looks the way it does.

| ADR | Decision | Why it matters to you |
| :--- | :--- | :--- |
| 0001 | Modular monolith with `api`/`biz` split per domain | Boundaries are enforced by Maven modules + ArchUnit/Modulith gates, not by convention |
| 0002 | Spring Data JPA as the only ORM | One persistence model; no MyBatis/second ORM surprises |
| 0003 | React + MUI admin console | Dense data screens, a11y-tested primitives, swappable behind `components/ui` |
| 0004 | Open Core as separate artifacts | Apache-2.0 community artifacts do not depend on private editions |
| 0005 | `Result<T>` success bodies + RFC 7807 error bodies | Predictable clients; machine-readable errors with trace ids |
| 0006 | Plugin SPI for storage/payments/LLM/Excel/encryption/workflow | Extend by adding a module, not by editing the core |
| 0020 | Plugin golden path as a Maven module example | The example in this repository is the template |
| 0022 | Open-source release with provenance and DCO | Licence and contribution expectations are explicit |
| 0025 | Maven coordinates and starters | `strata-spring-boot-starter` + `strata-bom` are the supported entry points |
| 0030 | Production guardrails (non-root, fail-fast secrets, single process) | Bad production configuration fails at startup |
| 0035 | Single process serves API + SPA | One deployable unit, no CORS in the default topology |
| 0037 | PostgreSQL only | Predictable SQL, Flyway and row-level features; no database matrix |
| 0038 | Current release packaging | The repository builds one application + one image; module names are not editions |

| — | TOTP MFA is opt-in per user; recovery is an administrator reset | Secrets are AES-GCM encrypted and audited; no backup codes in 0.x. See [TOTP MFA](./mfa.md) |
| — | The admin console ships as a reference application, not an npm package | The console is opinionated and tightly coupled to the reference product; teams with another frontend consume the REST API |
| — | No Spring Initializr/Maven archetype integration in 0.x | Bootstrap from the minimal examples (Maven, Gradle, Kotlin) plus the BOM; an Initializr contribution is evaluated for 1.0 |

A decision becomes public when it affects consumers; implementation-only decisions stay in the private ADR set.
