# SSO provider guides

> One OIDC/SAML integration model, documented against the providers teams actually use. The generic flow and security
> model live in [SSO / OIDC](./sso-oidc.md) and [SAML 2.0](./saml.md); this page covers provider-side setup.

## Provider matrix

| Provider | Protocol | Claim mapping | Notes |
| :--- | :--- | :--- | :--- |
| Keycloak (self-hosted) | OIDC + SAML | `preferred_username`, `email`, `groups` | Shipped demo stack imports a realm; the fastest local test |
| Auth0 | OIDC | `sub`, `email`, custom claim namespaced (e.g. `https://strata/roles`) | Use "Regular Web Application" + PKCE |
| Microsoft Entra ID | OIDC | `oid`/`preferred_username`, `roles` app roles, `groups` | App registration + token configuration for optional claims |
| Okta | OIDC + SAML + SCIM | `sub`, `email`, groups claim via authorization server | Same Okta org can also drive SCIM provisioning |
| Google Workspace | OIDC | `sub`, `email`, `hd` | Restrict to a hosted domain in the mapping rules |

## Common setup steps

1. Create the application in the provider (web app / confidential client).
2. Redirect URI: `https://<your-host>/api/v1/system/auth/social/callback` (exact match, no wildcard).
3. Request scopes `openid profile email` plus your groups/roles scope.
4. Copy client id/secret and issuer/discovery URL into the application configuration
   (`strata.security.social.*` for social login, or the SSO provider configuration — see the
   [configuration reference](./configuration-reference.md)).
5. Map claims to Strata users: subject id (stable), username, email, groups → roles. Provisioning behaviour is
   configured (create on first login, sync each login, or manual).
6. Verify: login redirect, callback, JWT claims, menu visibility, logout and token refresh.

## Auth0

- Application type: **Regular Web Application**; enable Authorization Code + PKCE.
- Discovery: `https://<tenant>.<region>.auth0.com/.well-known/openid-configuration`.
- Custom claims must be namespaced (Auth0 rules/actions restrict top-level claims): put roles under
  `https://strata.example.com/roles` and map that name in the claim mapping.
- Test user must have the roles assigned in the Auth0 dashboard, not only in Strata.

## Microsoft Entra ID

- App registration → **Web** platform; redirect URI exactly the Strata callback.
- Token configuration → add optional claims `email`, `preferred_username`, and app roles/groups if used.
- Groups claim can be overage-limited for users in many groups; prefer app roles for role mapping in large tenants.
- Issuer: `https://login.microsoftonline.com/<tenant-id>/v2.0` (use the tenant-specific issuer in production).

## Okta

- OIDC: create an **OIDC Web Application**, redirect URI exact, enable PKCE, map groups through the authorization server.
- SAML: create a SAML 2.0 app, use Strata's SP metadata endpoint as the audience/ACS configuration.
- SCIM: generate a SCIM token in Strata, configure Okta provisioning with `/api/v1/scim/v2` (see [SCIM](./scim.md)).

## Keycloak (local reference)

The repository's local demo ships a Keycloak stack with an imported realm, so the whole flow is testable offline:

```bash
docker compose -f docker-compose.yml up -d keycloak
# open http://localhost:8090, sign in with the demo admin, browse the realm
```

## Troubleshooting

| Symptom | Likely cause |
| :--- | :--- |
| `redirect_uri mismatch` | URI differs by trailing slash/host/port, or the provider normalized the host |
| Login loop / state error | Session cookie blocked (cross-site), or state expired (check clock skew) |
| `invalid_client` | Wrong client secret, or the client is configured as public |
| User created without roles | Group/claim mapping not configured, or the claim is overage-truncated |
| 401 after login | JWT secret changed on the app (old tokens rejected) |
