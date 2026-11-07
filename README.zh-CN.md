# Strata

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![CI](https://github.com/simplezac1995/strata/actions/workflows/ci.yml/badge.svg)](https://github.com/simplezac1995/strata/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.simplezac1995/strata-spring-boot-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.simplezac1995/strata-spring-boot-starter)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

面向通用后台产品的模块化单体后端框架：RBAC、运维、通知、AI 工具链与插件 SPI，模块边界由 Maven、Spring Modulith
�?ArchUnit 强制约束�?
基于 Spring Boot 4.1�?*�?JDK 25 构建、在 Java 21+ 运行**（见下文「构建要求」），包含后端框架、React 管理端和可直�?运行的示例�?
## 一�?Demo

**在线体验�?* <https://demo.simplecharlie.dev> —�?账号 `admin` / `admin123`（共享评估实例，数据会定期重置，请勿存放真实信息）�?需要长期使用请按下面两行命令在本地起一份：

```bash
git clone https://github.com/simplezac1995/strata.git
cd strata
docker compose up --build
```

- 管理端：<http://localhost:3001>（账�?`admin` / `demo-admin-password`�?- API 文档�?http://localhost:8080/swagger-ui.html>

仅用于本地评估；部署前请更换所�?demo 密钥。可选的 Keycloak SSO 叠加�?
```bash
docker compose -f docker-compose.yml -f docker-compose.sso.yml up --build
```

登录页点�?**Continue with Keycloak**（`demo` / `demo123`）。demo 用户通过 Keycloak group 映射�?`admin` 角色�?
![SSO Demo](./docs/screenshots/sso-demo.gif)

## 截图

| 登录 | 仪表�?|
| :---: | :---: |
| ![登录](./docs/screenshots/login.png) | ![仪表盘](./docs/screenshots/dashboard.png) |

| 用户管理（RBAC�?| AI 对话 |
| :---: | :---: |
| ![用户管理](./docs/screenshots/users.png) | ![AI 对话](./docs/screenshots/ai-chat.png) |

| SSO 登录（Keycloak�?| SSO 登录后的控制�?|
| :---: | :---: |
| ![SSO 登录](./docs/screenshots/sso-login.png) | ![SSO 控制台](./docs/screenshots/sso-dashboard.png) |
| SAML 登录（Keycloak�?| SAML 登录后的控制�?|
| ![SAML 登录](./docs/screenshots/saml-login.png) | ![SAML 控制台](./docs/screenshots/saml-dashboard.png) |
| 在线 demo（登录页） | 在线 demo（Dashboard） |
| :---: | :---: |
| ![在线 demo 登录](./docs/screenshots/demo-login.png) | ![在线 demo Dashboard](./docs/screenshots/demo-dashboard.png) |

截图来自仓库自带�?`docker compose` 演示环境�?
## 特�?
- **一个依赖即可启�?*：starter 自动装配系统域（RBAC、用户、角色、菜单、运维、配置）、实体与仓库、安全组件、调度�?  Swagger UI，并�?Flyway 迁移交付数据库结构�?- **明确的模块边�?*：Maven 模块 + Spring Modulith + ArchUnit 校验；跨域调用只能依�?`*-api` 契约�?- **可选能力按模块/插件接入**：AI（模型、对话、知识库）、对象存储、支付、LLM 网关、Excel、字段加密、工作流�?  添加依赖 + 属性即可启用，无需修改扫描配置�?- **宿主集成�?*：`VerificationSender`、`BackupExecutor`、`AuditChangeApplier` 自带安全默认实现，部署时声明
  同名 bean 即可替换�?- **生产级基础设施**：JWT/RBAC 菜单权限、RFC 7807 错误体、trace id、限流、幂等、审计、Flyway、CycloneDX
  SBOM、JaCoCo 覆盖门槛，以及最小应用的 Dockerfile�?- **SCIM 2.0 供应**：Okta / Entra ID / OneLogin 可通过�?token 保护�?SCIM 端点创建/更新/停用用户，并把组同步�?RBAC 角色；支�?ETag/If-Match、复杂过滤与批量操作�?- **SSO / OIDC 登录**：授权码 + PKCE，服务端换取令牌，提供商 userinfo 映射为常�?JWT/RBAC 会话；provider groups 可映射为 RBAC 角色（自动开户，可选每次登录同步）�?- **SAML 2.0 登录**：SP 发起�?SAML，XML-DSig 验签、基�?metadata �?IdP 信任、按邮箱映射/自动开户，最终仍是同一�?JWT/RBAC 会话；不依赖 OpenSAML�?- **管理�?*：React 19 + MUI 9，RBAC 驱动导航、路由懒加载、i18n、可访问性测试�?
## 本仓库包含什么（版本边界�?
Strata 采用 Open Core，边界可以在本仓库里核实�?
- **Community —�?本仓库（Apache-2.0�?*：框架模块、starter、插�?SPI、可运行示例，以及通用 React 管理端�?- **Pro / Enterprise —�?不在本仓�?*：面向受监管业务的垂直模块。它们通过 Maven Central 消费已发布的框架；本仓库
  不依赖它们�?- **没有 SaaS 锁定**：这里发布的一切均�?Apache-2.0，可运行在自己的基础设施上；私有模块是独立的可选产品�?
**Case study**：该框架已用于一个真实高风险垂直的生产环境（支付、KYC 与负责任博彩合规），其业务模块保持私有�?以上只描述维护者自己的部署，不声称存在外部客户�?
## 仓库结构

| 路径 | 内容 |
| :--- | :--- |
| `strata/` | Maven 反应堆：框架模块、starter、插件、代码生成器、示�?|
| `strata-ui/` | React 管理�?|
| `docs/` | 架构、工程规范、接入指南与自动生成的配置参�?|
| `strata/examples/` | 最小可运行应用与模块黄金路�?|
| `scripts/` | 仓库策略门禁与配置参考生成器 |

## 谁在维护

Strata 目前�?**[@simplezac1995](https://github.com/simplezac1995)** 单人维护�?
- **优先级与计划**：[ROADMAP.md](./ROADMAP.md)；维护者职责与 bus factor：[MAINTAINERS.md](./MAINTAINERS.md)�?- **问题与提�?*：[GitHub Issues](https://github.com/simplezac1995/strata/issues) —�?bug、功能请求与使用问题都在这里�?- **安全漏洞**：不要开公开 issue，按 [SECURITY.md](./SECURITY.md) 私下报告�?- **响应预期**：这是维护者投入的项目，issue �?PR �?best-effort 处理，以天计而非以小时计；安全报告遵�?  SECURITY.md 的目标时限。小而聚焦、带测试�?PR 最容易 review�?
## 构建要求

**�?JDK 25 构建，在 Java 21+ 运行�?* 项目�?JDK 25 工具链编译、字节码目标�?Java 21（`maven.compiler.release=21`�?不使�?preview 特性），CI 会用 Java 21 运行时启动构建产物。后端需�?Maven 3.9+；管理端需�?Node 22 �?pnpm 11�?发布只保留单一 JDK 25 构建矩阵，不维护第二套工具链�?
## 快速开�?
```bash
git clone https://github.com/simplezac1995/strata.git
cd strata/examples/strata-minimal-app
mvn spring-boot:run    # �?JDK 25 构建（Java 21 字节码）；通过 STRATA_DB_* 连接 PostgreSQL 18；Swagger �?/swagger-ui.html
```

或在已有应用中引入已发布�?starter�?
```xml
<dependency>
  <groupId>io.github.simplezac1995</groupId>
  <artifactId>strata-spring-boot-starter</artifactId>
  <version>0.11.1</version>
</dependency>
```

starter �?`classpath:db/strata-framework` 交付框架 schema（`V1__strata_framework.sql` �?`V2__idempotency_record.sql`）；保留�?Flyway 位置，并�?`V3` 起追加自己的迁移。AI 与代码生成器在有依赖时自动装配�?
## 文档

- [在线文档站](https://docs.simplecharlie.dev) · [离线文档](./docs/README.md)
- [快速开始](./docs/tutorials/getting-started.md)
- [新增模块](./docs/tutorials/add-a-module.md)
- [AI 接入](./docs/tutorials/ai-integration.md)
- [框架对比](./docs/comparison.md)

- [架构](./docs/architecture.md)
- [工程规范](./docs/conventions.md)
- [接入指南](./docs/adoption.md)
- [SSO / OIDC](./docs/sso-oidc.md)
- [SAML 2.0](./docs/saml.md)
- [配置参考](./docs/configuration-reference.md)
- [路线图](./ROADMAP.md) · [维护者](./MAINTAINERS.md)
- [贡献指南](./CONTRIBUTING.md) · [安全策略](./SECURITY.md) · [变更日志](./CHANGELOG.md)

## 构建与测�?
```bash
cd strata
mvn -B verify                      # JDK 25 构建（Java 21 字节码）：编�?+ 测试 + Spotless + ArchUnit + JaCoCo

cd ../strata-ui
pnpm install --frozen-lockfile
pnpm check                         # 类型检�?+ lint + 测试 + i18n + 格式 + 构建 + 包体�?```

导出仓是框架的单一精选历史；消费该框架的私有产品仓不属于本项目�?
## 版本与发�?
语义化版本，Maven Central 版本不可变：推�?`v*` tag 会触发发布流水线（GPG 签名、SBOM、Central Portal 发布）�?详见 [CHANGELOG.md](./CHANGELOG.md)�?
## 许可�?
Apache License 2.0 —�?�?[LICENSE](./LICENSE) �?[NOTICE](./NOTICE)�?