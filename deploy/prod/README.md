# Reference production deployment

A starting point for running Strata outside a laptop: one application process (API + admin SPA), PostgreSQL, Redis and
a TLS reverse proxy. Read [deployment](https://github.com/simplezac1995/strata/blob/main/docs/deployment.md),
[operations](https://github.com/simplezac1995/strata/blob/main/docs/operations.md) and
[security hardening](https://github.com/simplezac1995/strata/blob/main/docs/security-hardening.md) first.

## Quick start

```bash
cp deploy/prod/.env.example deploy/prod/.env    # fill every secret
docker compose -f deploy/prod/docker-compose.yml --env-file deploy/prod/.env up -d --build
```

The image build uses the repository root `Dockerfile` (multi-stage: JDK 25 build, Java 21 JRE runtime, non-root).
Caddy obtains a TLS certificate for `STRATA_DOMAIN` automatically; point the DNS record at this host first.

## What this is not

- Not a HA topology: one app instance, one PostgreSQL primary. Scale the stateless app behind the proxy, give every
  concurrent JVM a unique `STRATA_WORKER_ID`, and use managed PostgreSQL/Redis for production-critical workloads.
- Not a backup solution: add `pg_dump` schedules, off-host copies and restore drills (see operations guide).
- Not an observability stack: pair it with [deploy/observability](../observability/README.md).

## Operations

```bash
docker compose -f deploy/prod/docker-compose.yml --env-file deploy/prod/.env ps
docker inspect --format '{{.State.Health.Status}}' strata-app-1   # container name depends on compose project
curl -fsS http://127.0.0.1:8081/actuator/health
```
