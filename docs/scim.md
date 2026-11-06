# SCIM 2.0 provisioning

> Let an identity provider (Okta, Microsoft Entra ID, OneLogin, …) create/update/suspend users and groups in Strata
> through the SCIM 2.0 protocol. **Disabled by default** and authenticated with a dedicated bearer token, not JWTs.

## Configuration

```yaml
strata:
  scim:
    enabled: true
    token: ${SCIM_TOKEN}   # long random secret; the IdP sends "Authorization: Bearer <token>"
    max-results: 200
```

Keep the token in an environment variable/secret manager, rotate it like any other credential, and only expose
`/scim/v2/**` over TLS (typically behind the IdP's outbound proxy or an internal gateway). When the token is empty the
filter fails closed and every SCIM request is rejected with 401.

## Endpoints

Base path: `/scim/v2` (content type `application/scim+json`).

| Method | Path | Purpose |
| :--- | :--- | :--- |
| GET | `/ServiceProviderConfig` | Capabilities (PATCH + filter supported) |
| GET | `/Users?filter=…&startIndex=1&count=100` | List users (`userName eq`, `emails.value eq`, `active eq`) |
| POST | `/Users` | Create user (userName, name, emails, active, password) |
| GET/PUT/PATCH/DELETE | `/Users/{id}` | Read / replace / patch (add, replace, remove) / soft-delete |
| GET | `/Groups?filter=displayName eq "…"` | List groups (framework roles) |
| POST | `/Groups` | Create group (= role) and optional members |
| GET/PUT/PATCH/DELETE | `/Groups/{id}` | Read / replace / patch members / delete group |

## Mapping

| SCIM resource | Framework entity |
| :--- | :--- |
| User `userName` / `emails[primary]` | `sys_user.username` / `sys_user.email` |
| User `active` | `sys_user.enabled` |
| User `groups` | user roles (`sys_user_role`) |
| Group `displayName` | `sys_role.name` (code auto-derived as `scim-<slug>`) |
| Group `members` | role assignments |

Deleting a user or group is a **soft delete** (records stay for audit); the account is disabled and role links are
removed. Users without a local password cannot sign in with a password — pair provisioning with SSO/OIDC.

## IdP setup (example: Entra ID / Okta)

1. Create a SCIM/automation app in the IdP.
2. Base URL: `https://admin.example.com/scim/v2`; bearer token: the value of `strata.scim.token`.
3. Provision users with `userName`, `name.formatted`, `emails[primary].value`, `active`.
4. Provision groups and their members if you want IdP groups to manage Strata roles; assign roles to those groups in
   Strata's RBAC screens (or map them through OIDC group mapping for SSO users).

## Limitations

- No bulk operations, ETags/If-Match or complex filters (`and`/`or`, `gt`, `co`, …); only the simple `eq` filters above.
- One tenant per deployment: SCIM-created users/groups live in the default tenant.
- Group display names are not unique across tenants (role codes are unique); renaming a group changes the role name.

## Try it locally with the demo

The bundled demo enables SCIM with a demo token. Start it as usual, then:

```bash
docker compose up --build
TOKEN=strata-demo-scim-token
curl -s http://localhost:8080/scim/v2/ServiceProviderConfig -H "Authorization: Bearer $TOKEN"

# create a provisioned user
curl -s -X POST http://localhost:8080/scim/v2/Users \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/scim+json" \
  -d '{"schemas":["urn:ietf:params:scim:schemas:core:2.0:User"],"userName":"scim-demo",
       "name":{"formatted":"SCIM Demo"},"emails":[{"value":"scim@example.com","primary":true}],"active":true}'
```

The token is an evaluation default only — change it (and enable SCIM deliberately) in any real deployment.

## Verify manually

```bash
TOKEN=... # strata.scim.token
curl -s http://localhost:8080/scim/v2/ServiceProviderConfig -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8080/scim/v2/Users?filter=userName%20eq%20%22admin%22" -H "Authorization: Bearer $TOKEN"
```
