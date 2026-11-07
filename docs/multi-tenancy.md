# Multi-tenancy

> Read this before designing tenant isolation on top of Strata.

## What the community build ships

- **Directory-level tenancy**: tenant/organisation entities, tenant + user assignment and the RBAC model are provided by
  the system domain. A single deployment can serve multiple tenants when your application scopes data by tenant at the
  directory/data-access layer.
- **Row-level isolation is a private/Pro capability**: `strata.tenant.row-level-enabled=true` makes APIs scope rows by the
  `X-Tenant-Id` header. That implementation is not part of this open-source repository, so treat it as an extension point,
  not as a shipped feature.

## Recommended pattern for the community build

1. Model tenants as top-level rows owned by the system domain; keep a tenant id on every business table.
2. Enforce the tenant scope in the `*-api` service layer, not in controllers: derive the tenant from the authenticated
   principal and reject header/body tenant ids that do not match.
3. Add a database-level backstop: PostgreSQL row-level security policies or schema-per-tenant, depending on your
   operational preferences.
4. Test with two tenants in CI: one tenant must never return another tenant's rows, including through list/search
   endpoints and exports.
5. Keep caches and rate-limit keys tenant-prefixed; JWT claims carry the tenant id.

## What is not supported

- No cross-region routing or per-tenant database routing is shipped; multi-region is a deliberate non-goal in 0.x.
- The player/casino vertical is a separate private product; this repository's admin domain is staff-facing.
