# Load-testing scripts

k6 scripts used for the published baseline in [docs/performance.md](../../docs/performance.md). They are starting
points: the demo/login flow depends on your seeded users and environment variables.

```bash
export BASE_URL=https://admin.example.com
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD=...
k6 run deploy/testing/k6/read-list.js
k6 run -e VUS=20 -e DURATION=5m deploy/testing/k6/mixed.js
```

Always run against a staging copy with a production-like database. The numbers are workload-specific; do not publish
them as a guarantee.
