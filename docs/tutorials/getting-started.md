# Getting started

> From zero to a running admin backend in about ten minutes.

## 1. Run the demo (Docker Compose builds the images locally)

```bash
git clone https://github.com/simplezac1995/strata.git
cd strata
docker compose up --build
```

- Admin console: <http://localhost:3001> — login `admin` / `demo-admin-password`
- API documentation: <http://localhost:8080/swagger-ui.html>

The demo starts PostgreSQL, applies the framework schema and seeds the RBAC data. It is for local evaluation only;
change the secrets before any real deployment.

## 2. Create your own application

Add the starter to a Spring Boot 4.1 project running on **JDK 25** (the toolchain targets Java 21 bytecode, so the built application runs on Java 21+):

```xml
<dependency>
  <groupId>io.github.simplezac1995</groupId>
  <artifactId>strata-spring-boot-starter</artifactId>
  <version>0.11.1</version>
</dependency>
```

Write a plain application class — no component/entity scan is needed:

```java
@SpringBootApplication
public class MyAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyAdminApplication.class, args);
    }
}
```

Point it at PostgreSQL and provide the two required secrets:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/my_admin
    username: my_admin
    password: change-me
  flyway:
    locations: classpath:db/strata-framework,classpath:db/migration

strata:
  security:
    jwt:
      secret: ${STRATA_JWT_SECRET}     # at least 32 characters
  system:
    seed:
      admin-password: ${STRATA_ADMIN_PASSWORD}
```

The starter applies `db/strata-framework` (the framework schema: `V1__strata_framework.sql` and
`V2__idempotency_record.sql`). Flyway versions are global across all `spring.flyway.locations`, so append your own
migrations starting at `V3`.

## 3. Call the framework API

```bash
# login (returns accessToken) — for this application use admin / your STRATA_ADMIN_PASSWORD;
# for the demo stack from step 1 use admin / demo-admin-password
curl -s -X POST http://localhost:8080/api/v1/system/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<your-admin-password>"}'

# use the token
curl -s http://localhost:8080/api/v1/system/users -H "Authorization: Bearer $TOKEN"
```

Other entry points worth knowing: `/api/v1/system/menus`, `/api/v1/ops/announcements`, `/api/v1/ai/models`
(when the AI modules are on the classpath).

## 4. Run the admin console against it

The console is part of this repository. From the repository root (the clone from step 1):

```bash
cd strata-ui
pnpm install
VITE_API_PROXY_TARGET=http://localhost:8080 pnpm dev
```

Open <http://localhost:5173>. The console signs in with the same seeded admin account.

## Troubleshooting

| Symptom | Fix |
| :--- | :--- |
| Flyway fails with missing schema | Keep `classpath:db/strata-framework` in `spring.flyway.locations` |
| Flyway reports a duplicate or out-of-order migration | Flyway versions are global across all `spring.flyway.locations`. The framework already ships `V1__strata_framework.sql` and `V2__idempotency_record.sql` in `classpath:db/strata-framework`, so number your own migrations from `V3` upward (or use a separate version range) and never reuse the framework versions. |
| 401 on every request | `strata.security.jwt.secret` must be ≥ 32 characters and identical across restarts |
| Admin UI shows empty menus | Check that the RBAC seed ran (fresh database, `strata.seed.enabled=true`) |
| Port 3001/8080 busy | Change the host ports in `docker-compose.yml` (or set `STRATA_UI_PORT` / `STRATA_API_PORT`) |

Next: [add your first module](./add-a-module.md) or [connect the AI modules](./ai-integration.md).
