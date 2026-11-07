# Upgrade and support policy

## Versioning

- Strata follows **Semantic Versioning intent** while staying in **0.x**: minor releases can still contain breaking
  changes. Breaking changes are called out in [CHANGELOG.md](../CHANGELOG.md) and, where the database is involved, require
  a migration path.
- Flyway migrations are **immutable**: a released migration is never edited; fixes add new files. This keeps upgrade and
  downgrade reasoning possible and is verified by the repository's schema-policy gate.
- Maven Central releases are immutable. Every release tag is immutable; the GitHub release and SBOM are attached per tag.

## Which version to run

- Run the **latest release** for security fixes; patch releases are the supported security line for the previous minor.
- The generated OpenAPI document reports the framework version so you can verify what is deployed.
- The runtime floor is Java 21+; the build floor is JDK 25 (CI is the single JDK 25 matrix).

## Upgrade procedure

1. Read the CHANGELOG entries between your version and the target.
2. Bump the `strata-bom` version (Maven: import BOM; Gradle: `platform(...)`) and rebuild.
3. Run `mvn -B verify` / `pnpm check` on your fork; apply the migrations against a copy.
4. Deploy the new image; Flyway migrates at startup. Keep the previous image tag for rollback.
5. Verify `/actuator/health`, login, and the admin console's core flows.
6. Rollback is only safe when the migration did not invalidate data the old build still reads — otherwise roll forward.

## Support windows

| Line | Security fixes | Notes |
| :--- | :--- | :--- |
| latest minor | yes | recommended for new and existing deployments |
| previous minor | patch-level, best-effort | upgrade when practical |
| 0.x older | no | please upgrade |

Support is best-effort and maintainer-driven; see [MAINTAINERS.md](../MAINTAINERS.md) for the response expectations.

## Deprecation

- Deprecated APIs remain for at least one minor release and are documented in the CHANGELOG.
- Database migrations are never rewritten; a "fix" is always a new migration file.
- The `*-api` contracts are the public surface; `*-biz` internals are private implementation and may change in a minor.
