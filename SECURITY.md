# Security Policy

## Supported versions

| Version | Security fixes |
| :--- | :--- |
| `main` | ✅ (current development line, `0.11.2`) |
| released tags | last minor only |

The project is in its 0.10.x phase; no long-term support is promised before 1.0. The latest stable version is recorded
in `VERSION` and documented in [`CHANGELOG.md`](CHANGELOG.md). The policy for fixes backported from `main` to releases
will be published together with the version policy.

## Reporting a vulnerability

**Do not disclose vulnerability details in a public issue.** Use GitHub's private vulnerability
reporting:

> Security → Advisories → *Report a vulnerability*

Include: affected version/module, reproduction steps or a minimal reproduction project, impact (privilege
escalation, cross-tenant or cross-account access, data integrity, denial of service) and any mitigation
you are aware of.

### Response targets

| Stage | Target |
| :--- | :--- |
| Acknowledgement | within 3 business days |
| Triage and severity decision | within 7 business days (including whether accepted and the fix window) |
| Fix and disclosure | advisory published after the fixed release, with credit to the reporter |

These are targets, not a warranty. The maintainer is listed in [`MAINTAINERS.md`](MAINTAINERS.md); the security
contact is the same person.

## Security architecture

- **Authentication and sessions**: stateless JWT access tokens signed with a rotated secret
  (`strata.security.jwt.secret`, optional `previous-secret` overlap), refresh tokens with a Redis-backed
  revocation list, and login failure locking (`strata.security.login-lock`). JWT parsing ignores the `kid`
  header; logs only carry an 8-character secret fingerprint.
- **Authorisation**: method security with a single authority vocabulary
  (`dev.strata.common.security.auth.Permissions`); every controller lives in a `web` package and is covered
  by an ArchUnit rule.
- **Rate limiting**: per-path token buckets executed before the security chain
  (`RateLimitFilter` / `TokenBucketRateLimiter` in `strata-common-core`); the distributed variant uses
  Redisson. Production deployments are expected to configure at least one rule.
- **Audit logging**: `AuditLogAspect` (`strata-common-security`) records controlled operations, and
  operation/login logs are queryable through the admin API.
- **Secrets at rest**: Jasypt (`ENC(...)` values, `PBEWITHHMACSHA512ANDAES_256`) with the password injected
  as an environment variable, never stored in the repository; field-level encryption is available through
  `FieldEncryptor` (`strata-common-security`).
- **Injection and XSS**: data access uses Spring Data JPA / parameterised queries; the HTTP layer is
  JSON-only with output encoding plus CSP. Input cleaning is intentionally not applied to raw JSON
  payloads (vendor callbacks, encrypted envelopes) because rewriting them breaks signatures.
- **Public paths**: modules declare their own unauthenticated paths through `PermitPathContributor`; the
  core security configuration contains no business paths.
- **Multi-tenancy**: directory-level tenancy by default; row-level isolation is an explicit switch
  (`strata.tenant.row-level-enabled`).
- **Supply chain**: Apache-2.0 SPDX headers on every source file, a CycloneDX SBOM produced by the release
  profile, and a release pipeline that signs with GPG before publishing to Maven Central.

## SecurityRuntimeGuard

The distribution application (private repository) ships `SecurityRuntimeGuard`, which fails fast at startup
for non-local profiles when any of the following is not true:

1. `STRATA_JWT_SECRET` is provided by the environment and is not a placeholder or a known default.
2. `STRATA_ADMIN_PASSWORD` is at least 16 characters and not a placeholder.
3. `STRATA_CORS_ALLOWED_ORIGIN_PATTERNS` explicitly lists front-end origins (no wildcard).
4. Refresh-token revocation is enabled (`strata.security.token.blacklist-enabled=true`).
5. Login-lock storage is Redis (`strata.security.login-lock.store=redis`).
6. API rate limiting is enabled with at least one rule.

`prod` inherits these contracts through the PostgreSQL profile group; the `perf` profile is the only
explicitly authorised exemption for load testing.

## Known security work items

Security claims are not completed by declaring them. The distribution repository tracks open items
(including multi-instance rate-limit thresholds, CSRF hardening beyond SameSite, account enumeration and MFA
coverage) in its security design document and production-readiness checklist. Reports touching those areas
go through the process above.

## Out of scope

Vulnerabilities in third-party dependencies should be reported upstream; if Strata's default configuration
amplifies the impact, we still welcome a report here.
