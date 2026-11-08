# Security hardening

> A deployment checklist for teams embedding Strata. The framework defaults are safe-by-default, but production
> decisions (secrets, TLS, network, retention) are yours. See also [SECURITY.md](../SECURITY.md) for the disclosure
> process and the security model.

## 1. Secrets and configuration

- [ ] `STRATA_JWT_SECRET` is random, ≥ 32 chars, unique per environment, stored in a secret manager.
- [ ] Rotation uses `STRATA_JWT_SECRET_PREVIOUS` for an overlap window, then removes the old secret.
- [ ] `STRATA_ADMIN_PASSWORD` is ≥ 16 chars, not a placeholder, and changed after first login.
- [ ] Database/Redis credentials come from the secret manager, not from committed YAML.
- [ ] Field encryption (AES plugin) is enabled for sensitive columns when your compliance scope requires it; the key is
  external, rotated on a schedule and never logged.
- [ ] The startup guards are kept: missing/placeholder secrets and wildcard CORS must fail startup in production.

## 2. Network and transport

- [ ] TLS terminates at your proxy/ingress; the app port is bound to localhost or a private network.
- [ ] The management port (8081) is not exposed publicly.
- [ ] CORS lists exact origins (no wildcard); cross-site state-changing requests are additionally constrained by the
  admin write guard.
- [ ] Rate-limit rules are configured for login and expensive endpoints; login-failure lockout uses Redis in clustered
  deployments.
- [ ] Security headers (CSP, X-Content-Type-Options, HSTS at the proxy) are present; cookies use `__Host-`/Secure/SameSite
  where enabled.

## 3. Authentication and authorization

- [ ] RBAC menus/authorities are reviewed; least-privilege roles are granted, not the seeded admin.
- [ ] MFA: per-user TOTP ships with the framework (opt-in). Enroll privileged admins, set `STRATA_ENCRYPTION_KEY` so secrets survive restarts, and keep the admin reset path for lost devices (see [MFA](./mfa.md)).
- [ ] OIDC/SAML SSO providers are configured with PKCE, exact redirect URIs, and group→role mapping validated.
- [ ] SCIM tokens (if enabled) are separate secrets with a rotation plan; SCIM stays disabled by default.
- [ ] Player-facing endpoints (if you use the casino vertical in the private product) are on a separate HTTP surface.

## 4. Data and audit

- [ ] Audit/log retention and export are configured per your policy; logs exclude credentials and full entities.
- [ ] Backups are encrypted, off-host and restore-tested.
- [ ] Idempotency keys are used for money/state-changing client retries.
- [ ] Multi-tenancy: directory-level tenancy is the community model; row-level isolation is a private/Pro opt-in, not
  shipped in this repository.

## 5. Supply chain

- [ ] The release SBOM (`bom.json`) is archived with each deployment.
- [ ] Dependabot/security advisories are reviewed; patch versions are applied on a schedule.
- [ ] The public CI Trivy gate and dependency review stay enabled; do not merge with a failing security job.
- [ ] Verify the Maven Central signature/checksum for the artifacts you deploy.

## 6. Known security work items (honest list)

This framework is 0.x. The following are **not** closed in the community build and must be evaluated before production:
no TOTP backup/recovery codes in 0.x (recovery is an admin reset); the MFA challenge store is single-JVM (5-minute TTL)
until it moves to Redis; CSRF hardening beyond SameSite cookies; account-enumeration resistance in some flows;
multi-instance rate-limit thresholds; CSP tuning for custom frontends. Track them in your own risk register, and see
[SECURITY.md](../SECURITY.md) for reporting a vulnerability.
