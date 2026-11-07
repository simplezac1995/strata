# Documentation

Start here:

Hosted documentation site: <https://docs.simplecharlie.dev>. Online evaluation instance: <https://demo.simplecharlie.dev> (`admin` / `admin123`, shared, resets periodically).

| Document | Content |
| :--- | :--- |
| [Getting started tutorial](./tutorials/getting-started.md) | demo, your first application, first API call, admin console |
| [Add a module](./tutorials/add-a-module.md) | entity �?repository �?service �?controller �?migration �?test |
| [AI integration](./tutorials/ai-integration.md) | optional AI modules, LLM plugin, provider config, custom gateway |
| [Code generator](./tutorials/code-generator.md) | field matrix → backend slice, Flyway and admin console snippets |
| [How Strata compares](./comparison.md) | honest comparison with JHipster, Vaadin, Django admin, Refine and friends |
| [SSO / OIDC](./sso-oidc.md) | enterprise single sign-on: config, endpoints, provisioning, troubleshooting |
| [SAML 2.0](./saml.md) | keycloak/okta/entra SAML login: config, security model, demo, troubleshooting |
| [SCIM 2.0 provisioning](./scim.md) | let Okta/Entra/OneLogin create users, groups and role assignments |
| [FAQ](./faq.md) | Java baseline, demo, editions, naming and contribution questions |
| [Deployment](./deployment.md) | build the image, required secrets, health checks, upgrades, backups |
| [Operations](./operations.md) | metrics/logs/traces, backup/restore, incident runbook, capacity |
| [Security hardening](./security-hardening.md) | production checklist, known work items |
| [Upgrade and support](./upgrade-guide.md) | version policy, upgrade procedure, support windows |
| [Multi-tenancy](./multi-tenancy.md) | community directory-level tenancy vs private row-level isolation |
| [Plugin development](./plugin-development.md) | SPI contracts, golden path and the example module |
| [Architecture decisions](./decisions.md) | public digest of the ADRs that shape the framework |

Reference documentation:

Hosted documentation site: <https://docs.simplecharlie.dev>. Online evaluation instance: <https://demo.simplecharlie.dev> (`admin` / `admin123`, shared, resets periodically).

| Document | Content |
| :--- | :--- |
| [Architecture](./architecture.md) | module model, layering, HTTP surface, request pipeline, extension points |
| [Engineering conventions](./conventions.md) | backend layering/naming/data/API rules, admin UI conventions, quality gates |
| [Adoption guide](./adoption.md) | modules, plugins, extension points, schema/migrations, AI integration |
| [Configuration reference](./configuration-reference.md) | every configuration property, generated from the code |

Build requirement: **JDK 25** (the only supported baseline �?no Java 21 support line). See the root
[README](../README.md#build-requirement).

Business-domain documents, deployment runbooks and ADRs belong to the private product repository.
