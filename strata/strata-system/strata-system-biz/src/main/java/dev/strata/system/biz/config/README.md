# `biz` 包分层（2026-09-05）

| 包 | 内容 |
| :--- | :--- |
| `config` | `@Configuration`（`SystemBizConfiguration`、`AsyncConfig`）与 `@ConfigurationProperties`（`CommunityFeaturesProperties`、`LoginLockProperties`、`TokenBlacklistProperties`） |
| `seed` | `ApplicationRunner` 种子链（`SystemDataInitializer`、`SystemReferenceDataInitializer`、`SystemDemoDataInitializer`、`OpsDataInitializer`）、`OpsMenuSeedSupport`、seed JSON 结构（`MenuSeedFile`、`ReferenceDataSeed`）与 `SeedProperties` |
| `listener` | `@EventListener`（audit / login-log / operation-log） |
| `aspect` | `OperationLogAspect` + 包内 util `AuditLogSupport` |

其他域同构：`dev.strata.{ai|pro}.biz.seed` 承载各自的 `*SeedDataInitializer` 与 `*OpsMenuSeedContributor`；
域 `service/support` 放横切支撑（system：`LoginLockService`/`TokenBlacklistService`/`UserTenantSupport`；pro：`CommunityWorkflowRunner`）。

## seed 执行链

`ApplicationRunner` beans run after the context is ready. `@Order` value **lower runs first**.

| Order | Class | Scope |
| :---: | :--- | :--- |
| 1 | `SystemDataInitializer` | Core RBAC, menus, demo product, dicts, params |
| 2 | `AiSeedDataInitializer` | AI admin seed (providers, models, knowledge, …) — depends on system users/menus |
| 3 | `OpsDataInitializer` | Jobs, workflows metadata, ops extras |

Do not reorder without checking foreign keys and menu references in seed JSON.
