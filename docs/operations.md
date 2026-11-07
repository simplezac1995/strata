# Operations

> Day-two concerns: metrics, logs, traces, backups and the incident runbook. The framework ships the plumbing; the
> observability stack is yours to run.

## 1. Metrics

- Micrometer metrics are exposed on the management port at `/actuator/prometheus` when a registry (Prometheus, OTLP) is
  on the classpath.
- Built-in business metrics cover request latency/outcomes, login failures, rate-limit rejections and idempotency
  replays (see `BusinessMetrics`).
- Useful panels: p95/p99 latency per controller family, 5xx ratio, auth failure ratio, rate-limit rejections, DB pool
  saturation, Flyway migration duration, JVM heap/GC.

## 2. Logs and traces

- Application logs are structured and include the MDC trace id; the HTTP response carries `X-Trace-Id`.
- Production configuration should emit JSON logs (Loki/ELK friendly).
- Traces are OpenTelemetry-compatible: add your exporter agent/bean and correlate with the trace id header.
- Never log credentials, raw tokens or full entities with secrets.

## 3. Backup and restore

```bash
# backup (keep the sha256 alongside the file)
pg_dump -Fc -h $DB_HOST -U $DB_USER strata -f strata-$(date +%Y%m%d-%H%M).dump
sha256sum strata-*.dump > strata-latest.dump.sha256

# restore into an empty database and verify
createdb -h $DB_HOST -U $DB_USER strata_restore
pg_restore --no-owner --exit-on-error -h $DB_HOST -U $DB_USER -d strata_restore strata-*.dump
psql -h $DB_HOST -U $DB_USER -d strata_restore -c "select count(*) from information_schema.tables where table_schema='public'"
```

- Verify checksums before restoring; never restore over the live database.
- Redis is ephemeral: losing it invalidates refresh-token state and rate-limit windows, not business data.
- Run a restore drill at least quarterly and record the result in your ops notes.

## 4. Runbook

| Symptom | First checks | Action |
| :--- | :--- | :--- |
| Process exits on startup | log contains the startup guard message | fix the reported secret/CORS/JWT value; the guard fails fast on purpose |
| 401 on every request | `STRATA_JWT_SECRET` changed or rotated without the previous secret | restore the old secret or set `STRATA_JWT_SECRET_PREVIOUS` for the overlap window |
| Login failures with 429 | login-failure lockout or rate limit | inspect the lock store and rate-limit rules; wait out or clear the key |
| Duplicate POST rejected | idempotency key reuse | reuse the same `Idempotency-Key` only for retries of the same request |
| DB connection pool exhausted | slow queries, leaked transactions | check p99 DB latency and long-running transactions; increase pool only after fixing the cause |
| Redis unavailable | rate limiting/locks degrade | restore Redis; the API stays up but distributed locks and limiters are disabled �?consider fail-open/closed policy explicitly |
| Disk full | logs, heap dumps, backups | rotate logs, move backups off-host, alert on `df` |
| Certificate expiry | proxy TLS | renew via your ACME client; monitor 14 days before expiry |

## 5. Capacity guidance

There is no published benchmark because the load shape of an admin console is workload-specific. To size a deployment:

1. run a load test (k6/Vegeta) against a staging copy with realistic list/detail traffic;
2. watch p95 latency, DB CPU and pool saturation; scale the app horizontally (stateless) and add PostgreSQL headroom
   before adding replicas of Redis;
3. keep the management port on a private interface and separate its scrape/probe traffic from the public traffic.

## 6. Security patching

- Watch Dependabot/security advisories; patch Spring Boot/BOM patch versions on a schedule.
- The release pipeline signs artifacts with GPG, publishes a CycloneDX SBOM and publishes sources/javadoc to Maven
  Central; the GitHub release carries the `bom.json` asset.
- The public repository CI runs a Trivy dependency-lockfile scan and dependency review on pull requests; Maven\ndependency risk is covered by Dependabot and the signed release SBOM.
