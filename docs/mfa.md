# TOTP MFA

> Per-user one-time-password second factor. It is built into the framework (no extra module): enrollment happens from
> the admin console, the login flow returns an MFA challenge instead of a session, and secrets are encrypted at rest.

## What ships

- RFC 6238 TOTP: HMAC-SHA1, 6 digits, 30-second step, ±1 step clock-skew window (no external TOTP library).
- Per-user enrollment with a QR code (rendered server-side as a PNG data URL) and a manual Base32 key.
- **MFA-challenged login**: with MFA enabled, `POST /auth/login` returns `mfaRequired=true` plus a short-lived
  `mfaToken` and **null tokens**; the client exchanges the challenge and a code at `POST /auth/mfa/verify`.
- AES-256-GCM encryption of the stored secret through `FieldEncryptor`; the raw secret is never logged, never returned
  after enrollment and excluded from entity `toString()`.
- Self-service disable (requires a current code) and an administrator reset for lost devices.
- Audit entries for `auth:mfa` enable/disable/reset, plus a rate limit on the public verify endpoint.

## Configuration

| Property | Env variable | Default | Meaning |
| :--- | :--- | :--- | :--- |
| `strata.encryption.key` | `STRATA_ENCRYPTION_KEY` | empty (ephemeral dev key) | Base64-encoded 32-byte AES-256 key used to encrypt TOTP secrets. **Production with MFA must set it.** |
| `strata.mfa.issuer` | `STRATA_MFA_ISSUER` | `Strata Admin` | Label shown in authenticator apps and embedded in the `otpauth://` URI/QR. |

If the encryption key is empty the process logs a warning and uses a random per-process key: enrollment works, but the
secret cannot be decrypted after a restart or by another replica. Generate a key with
`openssl rand -base64 32` and store it in your secret manager.

## API

All response bodies use the framework envelope `Result<T>`; errors are RFC 7807 with a `code`.

| Method | Path | Auth | Purpose |
| :--- | :--- | :--- | :--- |
| POST | `/api/v1/system/auth/mfa/enroll` | session | Start enrollment: returns `secret`, `otpAuthUri`, `qrCodeDataUrl`, `mfaEnabled=false` |
| POST | `/api/v1/system/auth/mfa/enroll/confirm` | session | Body `{"code":"123456"}` — confirms enrollment and flips MFA on |
| POST | `/api/v1/system/auth/mfa/verify` | public | Body `{"mfaToken":"…","code":"123456"}` — completes a challenged login, returns tokens |
| POST | `/api/v1/system/auth/mfa/disable` | session | Body `{"code":"123456"}` — disables MFA for the current user |
| POST | `/api/v1/system/users/{id}/mfa/reset` | `system:user:update` | Administrator recovery: wipes the secret and disables MFA for another user |

### Login flow

```bash
# 1) password step
curl -s -X POST https://admin.example.com/api/v1/system/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"…"}'
# => {"code":0,"data":{"accessToken":null,"mfaRequired":true,"mfaToken":"<opaque>", …}}

# 2) second factor
curl -s -X POST https://admin.example.com/api/v1/system/auth/mfa/verify \
  -H 'Content-Type: application/json' \
  -d '{"mfaToken":"<opaque>","code":"123456"}'
# => {"code":0,"data":{"accessToken":"…","refreshToken":"…","user":…,"menus":…}}
```

Errors: `40112 MFA_REQUIRED`, `40113 MFA_INVALID_CODE`, `40114 MFA_CHALLENGE_EXPIRED`, `40115 MFA_NOT_ENROLLED`
(all HTTP 401). The challenge is single-use with a 5-minute TTL.

## Admin console

**Settings → Security → MFA**: the card starts enrollment (QR + manual key), confirms with a 6-digit code, and
disables with a current code. A user detail page exposes the administrator reset for locked-out accounts. The console
persists `user.mfaEnabled` in the auth store so the login screen and the lock screen can react to it.

## Limits and recovery

- **No backup/recovery codes in 0.x.** Recovery is an administrator reset (`POST /users/{id}/mfa/reset`, audited).
- The login challenge store is in-memory and single-JVM (5-minute TTL): it is fine for the supported single-instance
  deployment. Move it to Redis before running multiple replicas — the same class of known debt as the in-memory login
  lockout.
- MFA is per user and opt-in: there is no global "require MFA for everyone" switch in 0.x — enforce it through your
  onboarding/offboarding process or a custom guard.
- Enrollment and disable are session-authenticated; enroll privileged admins before enabling a forced-MFA policy.

## Tests

- `TotpServiceTest` — RFC 6238 vectors, Base32 round-trips and the ±1 step window.
- `TotpAppServiceTest` — enrollment, confirm, disable, reset and the encrypted-at-rest assertions.
- `MfaChallengeStoreTest` — issue/consume/expiry/single-use.
- `AuthAppServiceTest` — the challenged-login branch (no tokens until `mfa/verify` succeeds).
