# Adoption guide

> How to add a module, a plugin, a database migration and the optional AI domain to a Strata-based application.

## 1. Add a business module

Copy `examples/strata-module-example` and keep the three-way split:

```
my-domain/
  my-domain-api/    entities, DTOs, service ports
  my-domain-biz/    controllers, app services, repositories, mappers
    src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Rules: cross-module access goes through the other module's api; the biz module must not be imported by other domains;
every entity/repository is picked up by the starter's entity/repository scan when it lives under a scanned package.
Modules that need custom wiring ship their own `@AutoConfiguration` (see `ExampleAutoConfiguration`).

## 2. Write a plugin

Plugins implement a SPI from `strata-plugin-api` (or `strata-common-core`) and depend only on the SPI plus the
common kernel. Add an auto-configuration guarded by `@ConditionalOnProperty` so consumers enable the capability with
a property instead of a component scan. Reference implementations: local storage, Stripe, OpenAI-compatible LLM,
Excel, AES field encryption and Flowable.

## 3. Replace a framework extension point

The framework ships three host-integration SPIs with safe defaults:

| SPI | Default behaviour | Replace with |
| :--- | :--- | :--- |
| `VerificationSender` | logs that a code was requested | SMTP / SMS / notification service |
| `BackupExecutor` | records the intent, executes nothing | pg_dump + object storage / managed snapshots |
| `AuditChangeApplier` | refuses the replay (`false`) | restore an entity or route through an approval flow |

Declaring one bean replaces the default (the defaults are `@ConditionalOnMissingBean`).

## 4. Database schema and migrations

The starter ships the framework schema as a Flyway migration:

```
classpath:db/strata-framework/V1__strata_framework.sql
```

Keep that location and append your own migrations:

```yaml
spring:
  flyway:
    locations: classpath:db/strata-framework,classpath:db/migration
```

Never edit a released migration; add a new file. For a quick local demo you may use
`spring.jpa.hibernate.ddl-auto=create-drop` with `spring.flyway.enabled=false`, but that is not a production setup.
Entities and the shipped schema are validated against each other in CI (`FrameworkSchemaExportTest` +
Hibernate `validate`).

## 5. Optional AI domain

```xml
<dependency>
  <groupId>io.github.simplezac1995</groupId>
  <artifactId>strata-ai-biz</artifactId>
</dependency>
<dependency>
  <groupId>io.github.simplezac1995</groupId>
  <artifactId>strata-plugin-llm-openai</artifactId>
</dependency>
```

`AiAutoConfiguration` scans `dev.strata.ai` components, entities and repositories; the LLM plugin provides the
`LlmGateway` bean the AI modules require. Configure providers/models through the admin UI or
`strata.plugin.llm.openai-compatible.*`; keys belong in environment variables or the AES encryption plugin.

## 6. Single sign-on (OIDC)

Enable `strata.social.enabled`, register one or more providers under `strata.social.providers.*` and whitelist the
admin UI callback URI. The login page then shows one SSO button per provider; the callback exchanges the code
server-side, matches the user by verified email (optionally auto-provisioning with a default role) and issues the same
JWT/RBAC session as password login. Full configuration and troubleshooting: [SSO / OIDC](./sso-oidc.md).

## 7. Test the integration

Every example ships with tests that create their own PostgreSQL database, apply the framework migration and validate
the JPA mapping. Use the same `@DynamicPropertySource` pattern for your module tests, and run:

```bash
mvn -B verify
cd ../strata-ui && pnpm check
```
