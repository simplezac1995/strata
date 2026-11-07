# Maintainers

## Current maintainers

| Maintainer | Role | Areas |
| :--- | :--- | :--- |
| [@simplezac1995](https://github.com/simplezac1995) | Project owner, release manager, security contact | everything, including the private product modules |

## Responsibilities

- Triage issues and pull requests; keep `main` green and releaseable.
- Cut releases from `main`, publish to Maven Central, keep `CHANGELOG.md` current.
- Review security reports per [SECURITY.md](SECURITY.md) and publish advisories.
- Keep [ROADMAP.md](ROADMAP.md) honest and this file current.

## Response expectations (targets, not a warranty)

| Channel | Target |
| :--- | :--- |
| Security reports | acknowledgement within 3 business days; triage within 7 business days (see SECURITY.md) |
| Bug reports | triage within 10 business days |
| Pull requests | first review within 10 business days for focused PRs |
| Feature requests | batched into the roadmap; no individual SLA |

The maintainer funds this work personally: these are best-effort targets measured in days, not a support contract.
There is no paid support and no commercial SLA.

## Bus factor

The bus factor is **1**: the project currently has a single maintainer and no paid support. If the maintainer becomes
unavailable, the repository remains usable and forkable under Apache-2.0 and the published Maven Central artifacts
remain immutable. A continuity plan — a second maintainer or an explicitly documented successor — is a **1.0 exit
criterion** (see [ROADMAP.md](ROADMAP.md)). Until then, adopters should be able to build the framework from source.

## Becoming a maintainer

Sustained, high-quality contributions (code, documentation, reviews, issue triage) over time are the path. There is no
formal process yet; open an issue if you want to help own an area.
