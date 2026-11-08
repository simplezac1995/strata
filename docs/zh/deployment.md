# 部署（中文摘要）

> 完整细节见 [英文部署文档](../deployment.md)。

## 部署形态

一个 Spring Boot 进程同时提供 JSON API 与管理端 SPA；依赖 **PostgreSQL 18**（唯一支持的数据库）与 **Redis**。
运行环境 **Java 21+**，构建工具链 **JDK 25**。

## 参考部署

仓库提供 `deploy/prod/`：docker-compose（app + PostgreSQL + Redis + Caddy TLS）、`.env.example`、Caddyfile、nginx 示例。

```bash
cp deploy/prod/.env.example deploy/prod/.env    # 填写全部密钥
docker compose -f deploy/prod/docker-compose.yml --env-file deploy/prod/.env up -d --build
```

## 生产必填配置

| 环境变量 | 说明 |
| :--- | :--- |
| `STRATA_JWT_SECRET` | 随机 ≥32 字符，不得使用占位词；轮换用 `STRATA_JWT_SECRET_PREVIOUS` |
| `STRATA_ADMIN_PASSWORD` | 初始 admin 密码，≥16 字符，首次登录后修改 |
| `STRATA_CORS_ALLOWED_ORIGIN_PATTERNS` | 精确浏览器源，禁止通配符 |
| `STRATA_DB_*` | PostgreSQL 连接 |
| `STRATA_REDIS_HOST/PORT` | Redis 连接 |
| `STRATA_WORKER_ID` | 雪花 ID worker，[0,31]，每个并发 JVM 唯一 |
| `STRATA_MANAGEMENT_PORT` | 管理端口（默认 8081，仅内网） |

启动守卫会拒绝占位密钥/通配 CORS：配置错误时 fail-fast，不会带着不安全配置运行。Flyway 负责 schema，启动即迁移。

## 健康检查与升级

- `/actuator/health`（管理端口）；`/actuator/prometheus` 供抓取。
- 升级：读 CHANGELOG → 升 BOM → 在副本上跑迁移 → 部署新镜像 → 验证登录/核心流程；迁移不可回滚时优先前滚。
- 备份：`pg_dump -Fc` + 异地保存 + 定期恢复演练；Redis 仅存临时状态。

## 参考

- [运维（中文）](./operations.md) · [安全加固（中文）](./security-hardening.md)
- 英文：[Deployment](../deployment.md) · [Operations](../operations.md) · [Observability](../observability.md)
