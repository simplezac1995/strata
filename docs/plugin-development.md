# Plugin development

> Strata is extended by normal Spring Boot auto-configuration modules, not by touching the core. This page describes the
> SPI contracts, the golden path and the testing/opt-in rules. A runnable ~70-line example lives in
> `examples/strata-plugin-example`.

## 1. SPI contracts

| SPI | Package | When to implement |
| :--- | :--- | :--- |
| `ObjectStorage` | `dev.strata.plugin.api.storage` | File/object storage (S3, MinIO, local disk) |
| `PaymentGateway` | `dev.strata.plugin.api.payment` | Stripe/other checkout providers |
| `LlmGateway` | `dev.strata.plugin.api.llm` | LLM providers for the AI modules (chat/embeddings) |
| `SpreadsheetExporter` | `dev.strata.plugin.api.spreadsheet` | XLSX export |
| `WorkflowEngine` / `ProcessDefinitionCatalog` | `dev.strata.plugin.api.workflow` | BPMN execution (Flowable reference plugin) |
| `ApiPayloadCipher` | `dev.strata.plugin.api.crypto` | Encrypted payload envelopes for partner callbacks |
| `PluginStatusProvider` | `dev.strata.plugin.api.status` | Self-register a status entry in `GET /api/v1/plugins/status` |

Plugins depend only on `strata-plugin-api` + `strata-common-core` — never on another module's `-biz` implementation.
ArchUnit enforces this in the repository build.

## 2. Golden path

1. Create a Maven module depending on `strata-plugin-api` and `spring-boot-autoconfigure`.
2. Write an `@AutoConfiguration` class, guarded by a property (`@ConditionalOnProperty(prefix = "strata.acme", name = "enabled", havingValue = "true")`).
3. Register it in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
4. Provide the SPI bean(s) via `@Bean`; use `@ConditionalOnMissingBean` for overridable defaults.
5. Add a `PluginStatusProvider` so operators can see the plugin state in the ops UI.
6. Test with `ApplicationContextRunner`: assert the bean appears only when the switch is on.
7. Ship under Apache-2.0-compatible terms with SPDX headers; no core code changes are expected.

```java
@AutoConfiguration
@EnableConfigurationProperties(AcmeProperties.class)
@ConditionalOnProperty(prefix = "strata.acme", name = "enabled", havingValue = "true")
public class AcmeAutoConfiguration {
    @Bean
    public ObjectStorage acmeObjectStorage(AcmeProperties properties) {
        return new AcmeObjectStorage(properties);
    }
}
```

## 3. Registration and opt-in

- Disabled by default: a plugin neither registers beans nor contributes a status key until its property is on.
- Adding the jar is not enough: the host sets the property; that is deliberate (same rule as the community plugins).
- The `/plugins/status` endpoint aggregates every `PluginStatusProvider`; the key must stay camelCase
  (`storageLocal`, `paymentStripe`, `llmOpenAi`, `excel`, `encryptAes`, ...).

## 4. Example module

`examples/strata-plugin-example` contains the complete contract: auto-configuration, `PluginStatusProvider`, opt-in
property, tests and README. Copy it as the starting template for a real plugin, then replace the SPI bean with
`ObjectStorage`, `PaymentGateway` or `LlmGateway`.

## 5. Publishing a plugin

- Publish your own coordinates under your own groupId; the SPI artifact is stable in the 0.x line but pinned by the BOM.
- Document the property namespace you own (`strata.<your-plugin>.*`), the status key and the failure modes.
- Security-sensitive plugins (payments, storage) should document secret handling and never log payloads.
