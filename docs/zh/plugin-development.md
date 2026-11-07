# 插件开发（中文摘要）

> 完整指南与代码见 [英文插件指南](../plugin-development.md) 与 `examples/strata-plugin-example`。

## SPI 一览

| SPI | 用途 |
| :--- | :--- |
| `ObjectStorage` | 对象存储（S3/MinIO/本地） |
| `PaymentGateway` | 支付/收银台 |
| `LlmGateway` | LLM 对话/向量 |
| `SpreadsheetExporter` | XLSX 导出 |
| `WorkflowEngine` / `ProcessDefinitionCatalog` | BPMN 工作流 |
| `ApiPayloadCipher` | 回调报文加密 |
| `PluginStatusProvider` | 在 `GET /api/v1/plugins/status` 自注册状态 |

## 黄金路径

1. 新建 Maven 模块，依赖 `strata-plugin-api` + `spring-boot-autoconfigure`。
2. 写 `@AutoConfiguration`，用 `@ConditionalOnProperty(prefix="strata.<plugin>", name="enabled", havingValue="true")` 控制开关。
3. 在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册。
4. 通过 `@Bean` 提供 SPI 实现，可用 `@ConditionalOnMissingBean` 允许覆盖。
5. 提供 `PluginStatusProvider` 让运维在状态接口看到插件。
6. 用 `ApplicationContextRunner` 测试"开关关闭不注册 / 打开才注册"。
7. SPDX 头 + Apache-2.0 兼容许可，不修改核心代码。

参考实现：`examples/strata-plugin-example`（自动配置 + 状态提供者 + 测试 + README）。
