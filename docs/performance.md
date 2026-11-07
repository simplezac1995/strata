# Performance

> What the framework was measured at, how to reproduce it, and what the numbers do and do not mean.

## 1. Published baseline

Measured with k6 against a single application instance on a **20 vCPU / 64 GB** Docker host (2026-09), PostgreSQL 16,
Redis 7, warm JVM, seeded demo data. Workload: admin console traffic (JWT auth, list/detail reads, occasional writes).

| Scenario | Throughput | p95 latency | Error rate |
| :--- | :--- | :--- | :--- |
| User list pagination (read) | ≈ 1200 RPS | 8.2 ms | 0 |
| Create user (write) | ≈ 50 RPS | 76.6 ms | 0 |
| Mixed list/detail/write | ≈ 200 RPS | 93.2 ms | 0 |

Knee: ≈ 2000–4000 RPS on that hardware before latency degrades sharply. The admin console frontend is held to a
**single-chunk ≤ 600 kB** budget by `pnpm check`.

## 2. How to reproduce

```bash
export BASE_URL=https://admin.example.com
export ADMIN_USERNAME=admin ADMIN_PASSWORD=...
k6 run -e VUS=50 -e DURATION=3m deploy/testing/k6/read-list.js
k6 run -e VUS=20 -e DURATION=5m deploy/testing/k6/mixed.js
```

Run against a staging copy with production-like data, warm the JVM, and record JVM/DB/pool metrics alongside the k6
summary. Use at least two runs and discard cold-start.

## 3. What affects the numbers

- Database latency dominates p95 for reads; keep the DB on the same network and monitor Hikari pool saturation.
- Writes are bounded by transactions, Flyway/constraint checks and audit logging; batch imports are not free.
- JVM heap and GC: start at 2–4 GB for a 4–8 vCPU instance; measure, do not guess.
- The app is stateless: scale replicas horizontally and keep `STRATA_WORKER_ID` unique per JVM.
- Response compression (≥ 1 kB JSON) and HTTP/2 at the proxy materially affect perceived latency.

## 4. Caveats

These figures are a **starting point, not an SLA or benchmark guarantee**: they depend on hardware, database size,
indexes, query mix and enabled modules. Re-measure on your target hardware before capacity planning.

## 5. Tuning checklist

- PostgreSQL: `shared_buffers`, `effective_cache_size`, work_mem, connection pool sizing.
- HikariCP: pool size ≈ CPU cores × 2..4 for an admin workload; watch pending connections.
- JVM: `-Xms`=`-Xmx`, GC logs, container-aware memory limits.
- Redis: persistent connection reuse; no `KEYS`-style scans in hot paths.
- Proxy: keep-alive, compression, request timeouts.

See [Operations](./operations.md) for monitoring the resulting thresholds and [Deployment](./deployment.md) for the
reference topology.
