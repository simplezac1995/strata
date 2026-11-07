# Contributing to Strata

Thanks for taking the time to contribute. Strata is released under the Apache License 2.0 and uses a
**DCO sign-off (not a CLA)**. Before submitting anything, read [`docs/conventions.md`](docs/conventions.md) and
[`docs/architecture.md`](docs/architecture.md) — they are the authority for layout, layering, naming and module
boundaries. **Do not copy code from projects whose licence forbids redistribution** (see "Code provenance" below).

## Start here

| Order | Document | Purpose |
| :--- | :--- | :--- |
| 1 | [docs/conventions.md](docs/conventions.md) | directory structure, layering, naming, module dependencies, API shape and frontend layout (current state) |
| 2 | [docs/architecture.md](docs/architecture.md) | modular monolith, api/biz split, cross-cutting chains |
| 3 | [docs/configuration-reference.md](docs/configuration-reference.md) | every configuration property (generated from code) |

When documents disagree, [`docs/conventions.md`](docs/conventions.md) and [`docs/architecture.md`](docs/architecture.md)
win over everything else.

## Repository layout

This is the open-source framework repository. The framework modules (`strata-common`, `strata-system`, `strata-ai`,
`strata-plugin`, `strata-generator`, `strata-starter`) and `examples/strata-module-example` live in the `strata/`
Maven reactor; the generic admin console lives in `strata-ui/`. The deployable business product is a separate private
repository and is not part of this one.

## Branches

- `main` is the default branch and must always build and pass the quality gates.
- Use `feature/<area>-<short-description>` (for example `feature/starter-autoconfig`) and
  `fix/<area>-<problem>` for fixes.
- Branch from `main` and open a pull request against `main`; do not push to `main` directly.

## Local development

Prerequisites: **JDK 25** (the only supported build baseline — the project follows the latest LTS and does not offer
a Java 21 support line), Maven 3.9+, Node 22 and pnpm 11 (`packageManager: pnpm@11.7.0`). PostgreSQL 18 is the only
supported runtime database; Redis is required by rate limiting, locks and distributed coordination.

```bash
# Backend framework
cd strata
mvn -B -DskipTests install
mvn -B verify                          # compile + tests + Spotless + ArchUnit + JaCoCo

# Admin console
cd ../strata-ui
pnpm install
pnpm dev                               # UI on :5173, /api proxied to the backend you run
pnpm check                             # format + lint + typecheck + tests + build
```

The framework itself is a library set; the private distribution application assembles it with business
domains. `strata/examples/strata-module-example` shows how a feature module is built and tested without a
database.

## Quality gates

Everything below must pass before a pull request is merged:

```bash
# Backend: compile, tests, formatting, architecture rules, coverage floors
cd strata && mvn -B verify

# Frontend console: prettier, oxlint, audits, tsc, vitest, vite build, bundle-size budget, knip
cd strata-ui && pnpm check

# Repository-wide policy scripts
node scripts/verify-file-size.mjs      # 500-line source-file cap
node scripts/verify-spdx.mjs           # Apache-2.0 SPDX header on every source file
node scripts/generate-config-reference.mjs --check   # generated config reference is up to date
```

`mvn verify` is also an **architecture gate**: cross-domain dependencies are limited to `*-api`, `biz`
modules must not depend on other domains' `biz` modules, controllers live in `web` packages, and only the
application module may declare `@SpringBootApplication`. A failing build here is by design, not a false
positive.

## Commit messages

Conventional Commits, enforced in review (the local `commitlint` / `.husky` hooks live in the private product
repository and do not ship here):

```
type(scope): english summary
```

- **type**: `feat` `fix` `docs` `style` `refactor` `perf` `test` `build` `ci` `chore` `revert`
- **scope**: `backend` `frontend` `system` `rbac` `ops` `config` `communication` `ai` `plugin`
  `examples` `docs` `ci` `deps`
- English subject, lower-case first letter, at most 100 characters; body lines at most 100 characters.
- One commit does one thing; do not mix formatting, renames and behaviour changes.
- Examples: `fix(rbac): clear login failure cache`, `feat(example): add note pagination`.

Every commit must carry a DCO sign-off:

```bash
git commit -s -m "feat(ui): add paged query hook"
```

which appends `Signed-off-by: Your Name <your@email>` and certifies the
[DCO 1.1](https://developercertificate.org/): you have the right to submit the change under Apache-2.0.

## Pull request checklist

- [ ] `mvn -B verify` (backend) and `pnpm check` (admin console) are green locally.
- [ ] `node scripts/verify-file-size.mjs`, `node scripts/verify-spdx.mjs` and
      `node scripts/generate-config-reference.mjs --check` pass.
- [ ] Cross-domain code depends on `*-api` only; no `biz → other-domain biz` dependency.
- [ ] New endpoints live under `/api/v1/{module}/...`, use `@PreAuthorize(Permissions.X)` and only the
      `create | update | delete | list` permission actions.
- [ ] New authorities are added to `dev.strata.common.security.auth.Permissions` first and then granted in
      the RBAC seed data.
- [ ] Flyway migrations are append-only (new files only) and PostgreSQL-only. Number host migrations from `V3`:
      the framework already ships `V1` and `V2` in `classpath:db/strata-framework` and Flyway versions are global.
- [ ] Schema, configuration or permission changes are called out explicitly in the PR description.
- [ ] Documentation is updated when behaviour or configuration changes; architectural decisions get an ADR
      in the private product repository (the open-source repository only carries the framework contract
      documents).
- [ ] Every commit has a `Signed-off-by` line and a `type(scope): summary` subject.

## Code provenance (hard rule)

Strata is published under Apache-2.0, so the maintainers are responsible for every line in this
repository:

1. **Never copy** code from closed-source or restrictively licensed projects. Mechanisms and ideas may be
   reimplemented after review, but do not paste implementations, SQL or utility classes.
2. **Allowed sources**: Apache-2.0, MIT, BSD, EPL and similar permissive licences. Keep copyright and
   licence notices and record the attribution in [`NOTICE`](NOTICE).
3. **If in doubt, do not submit.** Rewrite it yourself rather than pasting a "harmless-looking" helper.

## Reporting issues

- Bugs and feature requests: GitHub Issues (include the version, reproduction steps and expected vs actual
  behaviour). The maintainer and response expectations are listed in [`MAINTAINERS.md`](MAINTAINERS.md).
- **Security vulnerabilities: never use a public issue** — see [`SECURITY.md`](SECURITY.md).
