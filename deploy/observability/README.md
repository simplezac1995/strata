# Observability bundle

Prometheus + Grafana + Loki + Promtail, provisioned for a Strata deployment. It is a local/single-host bundle, not a
managed monitoring platform.

## Quick start

```bash
export GRAFANA_ADMIN_PASSWORD='change-me'
docker compose -f deploy/observability/docker-compose.yml up -d
```

- Grafana: <http://localhost:3000> (dashboard "Strata / Strata overview", Prometheus + Loki datasources provisioned)
- Prometheus: <http://localhost:9090> (alerts in `alerts.yml`)
- Loki: <http://localhost:3100>

Edit `prometheus.yml` so the `strata-admin` target points at your management endpoint (`/actuator/prometheus`).
The reference deployment exposes it on `127.0.0.1:8081` of the Docker host.

## What is included

- Scrape config for the application (Micrometer -> Prometheus).
- Starting-point alert rules: app down, restarts, 5xx ratio, p95 latency, JVM heap, Hikari saturation, plus
  login-failure and rate-limit series when the business metrics are present.
- Grafana dashboard with request rate, 5xx, p95, heap, pool and login-failure panels.
- Docker log shipping through Promtail into Loki.

Recalibrate thresholds against your own load test before routing alerts to humans; see
[docs/observability.md](https://github.com/simplezac1995/strata/blob/main/docs/observability.md) and
[docs/operations.md](https://github.com/simplezac1995/strata/blob/main/docs/operations.md).
