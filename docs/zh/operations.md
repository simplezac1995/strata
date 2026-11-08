# 运维（中文摘要）

> 完整细节见 [英文运维文档](../operations.md)。

## 指标与日志

- Micrometer 指标：管理端口 `/actuator/prometheus`（HTTP 延迟/错误、JVM、Hikari、限流、登录失败、幂等重放）。
- 日志结构化并带 trace id；响应头 `X-Trace-Id` 可关联日志与调用链。
- 仓库提供 `deploy/observability/`：Prometheus + Grafana + Loki + Promtail 一键启动，含告警规则与看板。

```bash
export GRAFANA_ADMIN_PASSWORD='change-me'
docker compose -f deploy/observability/docker-compose.yml up -d
```

## 备份与恢复

```bash
pg_dump -Fc -h $DB_HOST -U $DB_USER strata -f strata-$(date +%Y%m%d).dump
sha256sum strata-*.dump > strata.dump.sha256
# 恢复到空库验证
createdb strata_restore && pg_restore --no-owner --exit-on-error -d strata_restore strata-*.dump
```

## 故障速查

| 现象 | 排查 | 处理 |
| :--- | :--- | :--- |
| 启动即退出 | 日志中的启动守卫信息 | 修正密钥/CORS/JWT 配置（守卫故意 fail-fast） |
| 全部 401 | JWT 密钥轮换未保留旧密钥 | 恢复旧密钥或设置 `STRATA_JWT_SECRET_PREVIOUS` |
| 登录 429 | 登录失败锁定/限流 | 检查锁定存储与限流规则，等待或清理键 |
| 重复 POST 被拒 | 幂等键复用 | 仅对同一请求的重试复用 `Idempotency-Key` |
| 连接池耗尽 | 慢查询/长事务 | 先查 DB 长事务与 p99，再调池大小 |
| Redis 不可用 | 限流/分布式锁降级 | 恢复 Redis；明确 fail-open/closed 策略 |
| 磁盘满 | 日志/备份 | 轮转日志、备份移出、设 `df` 告警 |

## 容量

没有放之四海的基准：用 `deploy/testing/k6` 在你的环境压测，观察 p95、DB CPU、连接池；应用无状态可水平扩容，
每个 JVM 使用唯一 `STRATA_WORKER_ID`。参见 [性能基线（英文）](../performance.md)。
