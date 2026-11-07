# Deployment

> How to build, configure and run Strata outside a laptop. The repository ships a demo Compose stack; this page
> describes the production shape, the required secrets and the upgrade path.

## 1. What you deploy

One Spring Boot process serves both the JSON API and the admin SPA (`classpath:/static`). The supported artifacts are:

- **Maven Central**: `io.github.simplezac1995:strata-spring-boot-starter` (or the `strata-bom`), Apache-2.0.
- **Container image**: the repository root `Dockerfile` builds a self-contained image (SPA + jar, non-root, Java 21
  JRE). Release tags are published to GHCR as `ghcr.io/simplezac1995/strata`.

Runtime floor: **Java 21+**. Build toolchain: **JDK 25**. Runtime dependencies: **PostgreSQL 18** (the only supported
database) and **Redis** (rate limiting, locks, token denylist, distributed state).

## 2. Build the image

```bash
git clone https://github.com/simplezac1995/strata.git
cd strata
docker build -t strata-admin:0.11.3 .
```

The image listens on **8080** (HTTP API + SPA). When you run the distribution application it also exposes a
management port (**8081**); keep it on the private network and scrape it from Prometheus there.

## 3. Minimal production-compose example

The repository's demo Compose file is a starting point, not a hardened production stack. A typical deployment is:

```yaml
services:
  db:
    image: postgres:18
    environment:
      POSTGRES_DB: strata
      POSTGRES_USER: strata
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes: [strata-db:/var/lib/postgresql]
  redis:
    image: redis:7-alpine
  app:
    image: strata-admin:0.11.3
    depends_on: [db, redis]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/strata
      SPRING_DATASOURCE_USERNAME: strata
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      STRATA_JWT_SECRET: ${STRATA_JWT_SECRET}
      STRATA_ADMIN_PASSWORD: ${STRATA_ADMIN_PASSWORD}
      STRATA_CORS_ALLOWED_ORIGIN_PATTERNS: https://admin.example.com
      STRATA_REDIS_HOST: redis
      STRATA_WORKER_ID: "1"
    ports: ["127.0.0.1:8080:8080", "127.0.0.1:8081:8081"]
volumes:
  strata-db:
```

Put a TLS-terminating reverse proxy (Caddy, nginx, your ingress) in front: browsers reach the API and SPA on the same
origin, so no CORS is needed in the default setup; set `STRATA_CORS_ALLOWED_ORIGIN_PATTERNS` only for page origins that
call the API cross-origin (never a wildcard).

## 4. Required configuration

The full property list is generated in [Configuration reference](./configuration-reference.md). The values you must
provide in production:

| Environment variable | Purpose |
| :--- | :--- |
| `STRATA_JWT_SECRET` | JWT signing secret, **≥ 32 characters**, not a placeholder, different per environment. Rotate with `STRATA_JWT_SECRET_PREVIOUS` for an overlap window. |
| `STRATA_ADMIN_PASSWORD` | Initial `admin` password (≥ 16 chars). Change it after first login. |
| `STRATA_CORS_ALLOWED_ORIGIN_PATTERNS` | Exact browser origins allowed to call the API (comma separated). |
| `STRATA_DB_HOST` / `PORT` / `NAME` / `USER` / `PASSWORD` | PostgreSQL connection (or `SPRING_DATASOURCE_*`). |
| `STRATA_REDIS_HOST` / `PORT` | Redis connection. |
| `STRATA_WORKER_ID` | Snowflake worker id `[0,31]`; **unique per running JVM**. |
| `STRATA_HTTP_PORT` / `STRATA_MANAGEMENT_PORT` | API port (8080) and management port (8081, internal only). |

The startup guards reject placeholder secrets and wildcard CORS: a badly configured production process fails fast
instead of running open. Flyway owns the schema; the application runs with `ddl-auto=validate` and never creates tables
itself.

## 5. Health, readiness and shutdown

- `GET /actuator/health` on the management port (liveness/readiness groups when enabled) — use it for probes.
- `GET /actuator/prometheus` when a Micrometer registry is on the classpath — scrape from the management network.
- Graceful shutdown is enabled; give the process the termination grace period your platform defaults to and drain the
  proxy first.
- The process is stateless: sessions are JWT, state lives in PostgreSQL/Redis, so rolling restarts are safe. Keep
  `STRATA_WORKER_ID` unique per concurrent JVM.

## 6. Upgrades and rollback

1. Read [CHANGELOG.md](../CHANGELOG.md) and the [upgrade guide](./upgrade-guide.md).
2. Bump the BOM/starter version; run the new image against a copy of production first.
3. Flyway migrations are **append-only and immutable**: the new image migrates the schema on startup. A rollback to an
   older image is only safe if the migration did not drop or rewrite columns the old build still reads — prefer
   forward fixes, and take a backup before every upgrade.
4. Keep the last known-good image tag; roll forward rather than down when a migration changed data.

## 7. Backups

- PostgreSQL: `pg_dump -Fc` on a schedule, upload off-host, verify the dump (`pg_restore --list`) and test a restore
  into an empty database at least quarterly.
- Redis holds only ephemeral state (locks, rate-limit buckets, token denylist); it does not need a backup, but losing it
  invalidates refresh tokens and rate-limit windows.
- Keep the application's `application-postgresql.yml`, env files and the encryption key (if the field-encryption
  plugin is used) in your secret manager, not in git.

## 8. Observability and operations

See [Operations](./operations.md) for metrics, logs, traces, backups and the incident runbook.
