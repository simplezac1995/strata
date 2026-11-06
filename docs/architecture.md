# Architecture

> A modular monolith: one deployable Spring Boot application, with module boundaries enforced at compile time and in
> tests by Maven, Spring Modulith and ArchUnit.

## 1. Shape

Strata is deliberately a **single process, single deployment unit**. Horizontal concerns (authentication, rate
limiting, idempotency, auditing, transactions) are Servlet filters, AOP aspects or Spring beans, never remote calls.
The only optional distributed dependencies are PostgreSQL and Redis (login locks, token denylist, distributed locks).

Choosing a modular monolith over microservices keeps the operational surface small while the module boundaries stay
explicit: they are verified on every `mvn verify`, so a later extraction along a module seam remains possible.

## 2. Modules

| Module | What it contains |
| :--- | :--- |
| `strata-common-*` | technical kernel: web/result, security, data/JPA, Redis, metrics, SPI contracts |
| `strata-system-*` | system domain: RBAC, operations (logs, tasks, backups), configuration (dicts, params), communication |
| `strata-ai-*` | optional AI domain: models/providers, chat, knowledge bases, tooling |
| `strata-plugin-*` | optional capabilities behind SPI: storage, payments, LLM gateways, Excel, field encryption, workflows |
| `strata-generator` | code generator (development tool, disabled by default) |
| `strata-spring-boot-starter` | auto-configuration, framework schema migration, Swagger, actuator wiring |

Every domain is split into two Maven modules:

| Module | Content | May be depended on by |
| :--- | :--- | :--- |
| `*-api` | entities, DTOs, service ports, domain enums/exceptions | other domains' `*-biz` |
| `*-biz` | controllers, app services, repositories, mappers | the assembly module only |

Dependency direction: `assembly → domain *-biz → own *-api → common`; a domain may depend on **another domain's
api module only**, never on its `-biz`. Plugins depend on `strata-plugin-api` + `strata-common-core`, never on
business implementations.

## 3. Layering inside a domain

| Layer | Package | Responsibility |
| :--- | :--- | :--- |
| Controller | `...biz.web` | binding, validation, `@PreAuthorize`, calls the app service, returns `Result` |
| AppService | `...biz.appservice` / `...biz.service` | use-case orchestration, transaction boundary |
| Repository | `...biz.repository` | Spring Data JPA interfaces |
| Domain / DTO | `...api.entity`, `...api.dto` | entities and request/response objects |

Rules: controllers never inject repositories, DTO↔entity mapping goes through MapStruct, an HTTP entry point lives in
`...biz.web` only, and a type is defined exactly once.

## 4. HTTP surface

```
/api/v1/<domain>/...     authenticated staff API (JWT + menu/authority strings)
```

- Success bodies use `Result<T>`; errors use RFC 7807 `application/problem+json` with
  `type/title/status/detail/code/timestamp/traceId`.
- Business errors are registered in `ErrorCode` and raised as framework exceptions; controllers never build JSON by
  hand and never use `PATCH`.
- Authorities follow `module:entity:action` (`create|update|delete|list`), referenced through the
  `Permissions` constants.
- Public callbacks/webhooks declare their paths through `PermitPathContributor`; the security assembly aggregates
  them instead of hard-coding URLs.
- URI versioning (`/api/vN/...`) supports deprecated-version headers driven by configuration.

## 5. Request pipeline

```
trace id → rate limit → Spring Security (CORS, headers, JWT, @PreAuthorize) → idempotency
       → sanitisation → DispatcherServlet → controller → @Transactional app service → repository
       → Result / ProblemDetails → audit aspect
```

## 6. Extension points

- **Plugins**: self-contained modules implementing a SPI (`StorageProvider`, `PaymentGateway`, `LlmGateway`,
  field encryption, workflow engine) and their own auto-configuration, enabled by property.
- **Framework SPIs with safe defaults**: `VerificationSender` (email/SMS codes), `BackupExecutor` (database
  backups) and `AuditChangeApplier` (audit replay). The default beans log and do nothing; declare your own bean to
  replace them.
- **Starter auto-configuration**: adding the starter wires the system domain, its entities/repositories, scheduling,
  security components, the framework schema (Flyway) and Swagger. Optional AI/generator modules wire themselves when
  their jars are present.

## 7. Explicit non-goals

No microservices, no service discovery/gateway/Feign/Dubbo, no OSGi-style runtime hot-plugging, no second ORM or
second UI framework, no backward-compatibility shims for internal APIs.
