# Tutorial: AI integration

> Goal: enable the optional AI domain (models, chat, knowledge bases) on top of the starter.

## 1. Add the dependencies

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

`strata-ai-biz` brings the chat/knowledge/config modules and `AiAutoConfiguration` scans `dev.strata.ai`
(components, entities, repositories). The LLM plugin provides the `LlmGateway` bean the AI modules require.

## 2. Configure a provider

```yaml
strata:
  plugin:
    llm:
      openai-compatible:
        enabled: true
        base-url: https://api.openai.com/v1
        api-key: ${OPENAI_API_KEY}
```

Additional providers/models can be managed in the admin UI (`/api/v1/ai/providers`, `/api/v1/ai/models`). Keep the
API key out of YAML: use environment variables or the AES field-encryption plugin.

## 3. Try the API

```bash
# list models (requires ai:model:list authority)
curl -s http://localhost:8080/api/v1/ai/models -H "Authorization: Bearer $TOKEN"

# create a chat session
curl -s -X POST http://localhost:8080/api/v1/ai/chat/sessions \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"demo"}'
```

Knowledge bases live under `/api/v1/ai/knowledge-bases`; documents are uploaded and chunked into the configured vector
store.

## 4. Swap the LLM gateway

The OpenAI-compatible gateway is one implementation of `LlmGateway`. To use another vendor, declare your own bean:

```java
@Configuration
class MyLlmConfiguration {
    @Bean
    LlmGateway llmGateway() {
        return new MyVendorLlmGateway();
    }
}
```

The AI modules use the gateway through the SPI, so no other code changes.

## 5. Permissions and menus

AI pages use `ai:*` authorities (`ai:chat:list`, `ai:knowledge:list`, `ai:model:list`, ...), seeded with the RBAC
data. Grant them to roles through the admin UI.

## 6. Tests

The minimal application already covers the whole chain (Flyway schema → Hibernate validate → AI auto-configuration →
Swagger groups). Run:

```bash
mvn -B -pl examples/strata-minimal-app -am -Dtest=MinimalApplicationSmokeTest test
```

Add an `@SpringBootTest` with a fake `LlmGateway` for your own AI flows.
