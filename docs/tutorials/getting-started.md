# Getting started

> From zero to a running admin backend in about ten minutes.

## 1. Run the demo (no build required)

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

Add the starter to a Spring Boot 4.1 / Java 25 project:

```xml
<dependency>
  <groupId>io.github.simplezac1995</groupId>
  <artifactId>strata-spring-boot-starter</artifactId>
  <version>0.3.0</version>
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

The starter applies `db/strata-framework` (the framework schema). Append your own migrations after it.

## 3. Call the framework API

```bash
# login (returns accessToken)
curl -s -X POST http://localhost:8080/api/v1/system/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"demo-admin-password"}'

# use the token
curl -s http://localhost:8080/api/v1/system/users -H "Authorization: Bearer $TOKEN"
```

Other entry points worth knowing: `/api/v1/system/menus`, `/api/v1/ops/announcements`, `/api/v1/ai/models`
(when the AI modules are on the classpath).

## 4. Run the admin console against it

```bash
cd ../strata-ui
pnpm install
VITE_API_PROXY_TARGET=http://localhost:8080 pnpm dev
```

Open <http://localhost:5173>. The console signs in with the same seeded admin account.

## Troubleshooting

| Symptom | Fix |
| :--- | :--- |
| Flyway fails with missing schema | Keep `classpath:db/strata-framework` in `spring.flyway.locations` |
| 401 on every request | `strata.security.jwt.secret` must be ≥ 32 characters and identical across restarts |
| Admin UI shows empty menus | Check that the RBAC seed ran (fresh database, `strata.seed.enabled=true`) |
| Port 3000/8080 busy | Change the host ports in `docker-compose.yml` |

Next: [add your first module](./add-a-module.md) or [connect the AI modules](./ai-integration.md).
