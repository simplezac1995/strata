# strata-plugin-example

Minimal plugin module for the framework, used by the
[plugin development guide](https://github.com/simplezac1995/strata/blob/main/docs/plugin-development.md).

It shows the whole plugin contract in ~70 lines:

1. a normal Spring Boot auto-configuration class (`ExamplePluginAutoConfiguration`);
2. opt-in by property (`strata.example-plugin.enabled=true`), off by default;
3. one SPI bean (`PluginStatusProvider`) that `GET /api/v1/plugins/status` picks up automatically;
4. registration through `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`;
5. tests that prove the bean appears only when enabled.

## Use it

```xml
<dependency>
  <groupId>io.github.simplezac1995</groupId>
  <artifactId>strata-plugin-example</artifactId>
  <version>${project.version}</version>
</dependency>
```

```yaml
strata:
  example-plugin:
    enabled: true
    detail: wired by the host application
```

The module is an example, not part of the published starter; copy the layout and replace the SPI bean with
`ObjectStorage`, `PaymentGateway`, `LlmGateway`, `SpreadsheetExporter` or `WorkflowEngine` as needed.
