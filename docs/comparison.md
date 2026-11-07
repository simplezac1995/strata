# How Strata compares

> An honest overview. Strata is young (first public release 2026) and Spring-only; the table shows where it fits and
> where other tools are the better choice.

| Dimension | Strata | JHipster | Spring PetClinic | Vaadin | Django admin | NestJS + React admin | Refine / Directus / Payload |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Stack | Spring Boot 4.1 / Java 21+ (built with JDK 25) + React 19 / MUI 9 | Spring Boot (generated) + React/Angular/Vue | Spring Boot reference app | Java + Vaadin Flow | Python / Django | NestJS + React | TypeScript / headless or monolith |
| Architecture | **Modular monolith with enforced boundaries** (Maven + Spring Modulith + ArchUnit) | Layers, no enforced domain boundaries | Teaching example | Component-based UI framework | MTV, apps | Modules, DI, no boundary enforcement | Varies |
| License | Apache-2.0 | Apache-2.0 | Apache-2.0 | Apache-2.0 / commercial | BSD | MIT | MIT / Apache / AGPL variants |
| Admin UI included | ✅ React admin console (RBAC menus, CRUD, i18n) | ✅ generated UI | ❌ | ✅ server-side UI | ✅ built-in | ❌ assemble your own | ✅ admin panel |
| Auth / RBAC | ✅ JWT + RBAC menus + authorities + SSO/OIDC/SAML 2.0 | ✅ | basic | add-on | ✅ basic | add-on | varies |
| AI tooling | ✅ models, chat, knowledge bases, LLM plugin SPI | ❌ | ❌ | ❌ | ❌ | ❌ | varies |
| Plugin SPIs | ✅ storage, payments, LLM, Excel, encryption, workflow | ❌ | ❌ | add-ons | apps | ❌ | extensions vary |
| Enforced build gates | ✅ ArchUnit, Spotless, JaCoCo floors, SBOM, policy scripts | partial | ❌ | partial | ❌ | optional | optional |
| Learning curve | Medium (Spring + React + DDD-lite) | Medium | Low | Medium | Low | Medium | Low–medium |
| Production readiness | Young project; 0.x, API may change | Mature, large community | Example only | Mature commercial | Mature | Mature ecosystem | Mature |
| Best fit | Java teams building an admin/ops product with strict module boundaries and AI features | Teams that want a generated full-stack scaffold | Learning Spring | Java teams that want server-side UI | Python teams needing a quick admin | Teams that prefer Node/TypeScript | TypeScript teams wanting a headless/admin backend |

## Where Strata is a good fit

- You are a **Java/Spring team** and want an admin/ops backend with RBAC, auditing, operations tooling and a React
  console — without assembling ten libraries yourself.
- You care about **enforced module boundaries** because the product will grow for years.
- You want **AI capabilities** (models, chat, knowledge bases) as part of the same modular monolith.
- You want **no SaaS lock-in** and a permissive license.

## Where another tool is the better choice

- Your team is not on Java/Spring → use Refine, Payload, Directus, Django admin or Laravel Nova.
- You need a UI framework rather than an admin product skeleton → Vaadin.
- You want a large, mature ecosystem and a stable API today → JHipster, or a commercial admin product.
- You need managed hosting and support immediately → a commercial platform.

## Current limitations (read before adopting)

- 0.x versioning: expect breaking changes until 1.0; migrations are immutable, APIs may still evolve.
- **Build with JDK 25; run on Java 21+.** The toolchain targets the Java 21 bytecode level and CI starts the built
  application on a Java 21 runtime; Spring Boot 4.1 is correspondingly new, so validate against your platform's
  support matrix.
- Only Chinese/English UI dictionaries ship today; the i18n layer accepts more.
- SSO/OIDC (authorization code + PKCE, multi-instance via Redis state store) and SAML 2.0 (SP-initiated, XML-DSig,
  metadata-driven) login are included, as is SCIM 2.0 provisioning; see [SSO / OIDC](./sso-oidc.md),
  [SAML 2.0](./saml.md) and [SCIM 2.0](./scim.md).
- One maintainer (see [MAINTAINERS.md](../MAINTAINERS.md)): response times and roadmap are deliberately limited —
  priorities are published in [ROADMAP.md](../ROADMAP.md).
