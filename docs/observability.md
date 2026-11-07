# Observability

> The framework ships the plumbing (Micrometer, structured logs, trace ids, business metrics); the monitoring stack is
> yours to run. A provisioned local bundle lives in `deploy/observability/`.

## 1. Metrics

- Endpoint: `/actuator/prometheus` on the management port (default 8081 when the distribution application runs).
- Series include HTTP server latency/outcomes, JVM memory/GC, HikariCP pool state, cache stats, rate-limit rejections,
  login failures and idempotency replays.
- Scrape interval 15s is a good starting point; keep the management port on a private interface.

## 2. Quick start with the bundled stack

```bash
export GRAFANA_ADMIN_PASSWORD='change-me'
docker compose -f deploy/observability/docker-compose.yml up -d
```

- <http://localhost:3000> — Grafana, with the *Strata overview* dashboard and Prometheus + Loki datasources provisioned.
- <http://localhost:9090> — Prometheus, alert rules loaded from `deploy/observability/alerts.yml`.
- <http://localhost:3100> — Loki; Promtail ships Docker container logs.

Point the `strata-admin` target in `prometheus.yml` at your management endpoint, then recalibrate alert thresholds
against your own load test (see [Performance](./performance.md)).

## 3. Logs

- Application logs include the request trace id (also returned as `X-Trace-Id`).
- Emit JSON logs in production for Loki/ELK ingestion; keep secrets out of log fields.
- Retention is yours to define (Loki retention or your platform's log policy).

## 4. Traces

- OpenTelemetry-compatible: add an OTLP exporter to the app (micrometer-tracing/OTel agent) and point it at your
  collector. The trace id correlates logs, errors and API responses.
- The management port and outbound OTLP traffic belong on the private network.

## 5. Alerting

`deploy/observability/alerts.yml` contains starting-point rules:

| Alert | Signal |
| :--- | :--- |
| StrataAppDown | `up{job="strata-admin"} == 0` |
| StrataAppRestarted | process start time changed |
| High5xxErrorRate | 5xx ratio > 5% for 5m |
| HighLatencyP95 | p95 > 1s for 10m |
| JvmHeapHigh | heap > 90% for 10m |
| HikariPoolSaturated / Exhausted | pending/active connections vs limits |

Route alerts through your alertmanager (Slack/PagerDuty/email) after a soak period; avoid paging on thresholds you have
not measured yourself.
