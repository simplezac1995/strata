# Strata

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![CI](https://github.com/simplezac1995/strata/actions/workflows/ci.yml/badge.svg)](https://github.com/simplezac1995/strata/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.simplezac1995/strata-spring-boot-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.simplezac1995/strata-spring-boot-starter)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

面向通用后台产品的模块化单体后端框架：RBAC、运维、通知、AI 工具链与插件 SPI，模块边界由 Maven、Spring Modulith
和 ArchUnit 强制约束。

基于 Spring Boot 4.1 与 Java 25，包含后端框架、React 管理端和可直接运行的示例。

## 截图

| 登录 | 仪表盘 |
| :---: | :---: |
| ![登录](./docs/screenshots/login.png) | ![仪表盘](./docs/screenshots/dashboard.png) |

| 用户管理（RBAC） | AI 对话 |
| :---: | :---: |
| ![用户管理](./docs/screenshots/users.png) | ![AI 对话](./docs/screenshots/ai-chat.png) |

截图来自仓库自带的 `docker compose` 演示环境。

## 特性

- **一个依赖即可启动**：starter 自动装配系统域（RBAC、用户、角色、菜单、运维、配置）、实体与仓库、安全组件、调度、
  Swagger UI，并以 Flyway 迁移交付数据库结构。
- **明确的模块边界**：Maven 模块 + Spring Modulith + ArchUnit 校验；跨域调用只能依赖 `*-api` 契约。
- **可选能力按模块/插件接入**：AI（模型、对话、知识库）、对象存储、支付、LLM 网关、Excel、字段加密、工作流，
  添加依赖 + 属性即可启用，无需修改扫描配置。
- **宿主集成点**：`VerificationSender`、`BackupExecutor`、`AuditChangeApplier` 自带安全默认实现，部署时声明
  同名 bean 即可替换。
- **生产级基础设施**：JWT/RBAC 菜单权限、RFC 7807 错误体、trace id、限流、幂等、审计、Flyway、CycloneDX
  SBOM、JaCoCo 覆盖门槛，以及最小应用的 Dockerfile。
- **SSO / OIDC 登录**：授权码 + PKCE，服务端换取令牌，提供商 userinfo 映射为常规 JWT/RBAC 会话（可自动开户并授予默认角色）。
- **管理端**：React 19 + MUI 9，RBAC 驱动导航、路由懒加载、i18n、可访问性测试。

## 仓库结构

| 路径 | 内容 |
| :--- | :--- |
| `strata/` | Maven 反应堆：框架模块、starter、插件、代码生成器、示例 |
| `strata-ui/` | React 管理端 |
| `docs/` | 架构、工程规范、接入指南与自动生成的配置参考 |
| `examples/` | 最小可运行应用与模块黄金路径 |
| `scripts/` | 仓库策略门禁与配置参考生成器 |

## 快速开始

```bash
git clone https://github.com/simplezac1995/strata.git
cd strata/examples/strata-minimal-app
mvn spring-boot:run    # 通过 STRATA_DB_* 连接 PostgreSQL 18；Swagger 在 /swagger-ui.html
```

或在已有应用中引入已发布的 starter：

```xml
<dependency>
  <groupId>io.github.simplezac1995</groupId>
  <artifactId>strata-spring-boot-starter</artifactId>
  <version>0.4.0</version>
</dependency>
```

starter 在 `classpath:db/strata-framework` 交付框架 schema；保留该 Flyway 位置并追加自己的迁移即可。AI 与代码生成器
在有依赖时自动装配。

## 文档

- [快速开始](./docs/tutorials/getting-started.md)
- [新增模块](./docs/tutorials/add-a-module.md)
- [AI 接入](./docs/tutorials/ai-integration.md)
- [框架对比](./docs/comparison.md)

- [架构](./docs/architecture.md)
- [工程规范](./docs/conventions.md)
- [接入指南](./docs/adoption.md)
- [SSO / OIDC](./docs/sso-oidc.md)
- [配置参考](./docs/配置参考.md)
- [贡献指南](./CONTRIBUTING.md) · [安全策略](./SECURITY.md) · [变更日志](./CHANGELOG.md)

## 构建与测试

```bash
cd strata
mvn -B verify                      # 编译 + 测试 + Spotless + ArchUnit + JaCoCo

cd ../strata-ui
pnpm install --frozen-lockfile
pnpm check                         # 类型检查 + lint + 测试 + i18n + 格式 + 构建 + 包体积
```

导出仓是框架的单一精选历史；消费该框架的私有产品仓不属于本项目。

## 版本与发布

语义化版本，Maven Central 版本不可变：推送 `v*` tag 会触发发布流水线（GPG 签名、SBOM、Central Portal 发布）。
详见 [CHANGELOG.md](./CHANGELOG.md)。

## 许可证

Apache License 2.0 —— 见 [LICENSE](./LICENSE) 与 [NOTICE](./NOTICE)。
