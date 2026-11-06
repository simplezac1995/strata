// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.biz.service;

import dev.strata.ai.api.request.AiChatAttachment;
import dev.strata.ai.chat.biz.config.AiChatProperties;
import dev.strata.ai.config.biz.repository.AiPromptRepository;
import dev.strata.plugin.api.llm.LlmChatResult;
import dev.strata.plugin.api.llm.LlmGateway;
import java.sql.Timestamp;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Executes the tool-call half of the chat pipeline: resolves the operator-editable system prompt, appends the
 * available-tools manifest, recognises {@code [TOOL:name {json}]} markers emitted by the model, gates write tools
 * behind user confirmation and runs both the built-in DB tools and generic in-process backend API calls.
 */
@Component
public class AiChatToolDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AiChatToolDispatcher.class);

    private final AiPromptRepository aiPromptRepository;
    private final LlmGateway llmGateway;
    private final AiChatProperties chatProperties;
    private final JdbcTemplate jdbc;

    public AiChatToolDispatcher(
            AiPromptRepository aiPromptRepository,
            LlmGateway llmGateway,
            AiChatProperties chatProperties,
            DataSource dataSource) {
        this.aiPromptRepository = aiPromptRepository;
        this.llmGateway = llmGateway;
        this.chatProperties = chatProperties;
        this.jdbc = dataSource == null ? null : new JdbcTemplate(dataSource);
    }

    /** Load the operator-editable chat system prompt from sys_ai_prompt (code = chat_system). */
    String resolveSystemPrompt() {
        try {
            return aiPromptRepository
                    .findByCodeAndEnabledTrue("chat_system")
                    .map(p -> p.getTemplate())
                    .filter(t -> t != null && !t.isBlank())
                    .orElse(LlmGatewaySupport.DEFAULT_SYSTEM_PROMPT);
        } catch (Exception e) {
            log.warn("Failed to load chat_system prompt, using default: {}", e.getMessage());
            return LlmGatewaySupport.DEFAULT_SYSTEM_PROMPT;
        }
    }

    /** Append the available-tools manifest to the system prompt so the model can emit {@code [TOOL:name {json}]}. */
    String withToolsManifest(String baseSystemPrompt) {
        StringBuilder sb = new StringBuilder(baseSystemPrompt == null ? "" : baseSystemPrompt);
        sb.append("\n\n## 可用工具\n");
        sb.append("当用户的请求需要查数据或执行操作时，你必须输出一行工具调用标记（不要解释，直接输出标记）：\n");
        sb.append("[TOOL:list_users {}] — 查询后台用户列表\n");
        sb.append("[TOOL:create_user {\"username\":\"...\",\"displayName\":\"...\"}] — 创建后台用户\n");
        sb.append("[TOOL:list_orders {}] — 查询最近充值记录\n");
        sb.append("[TOOL:api_call {\"method\":\"GET|POST|PUT|DELETE\",\"path\":\"/api/v1/...\",\"body\":{...}}] —"
                + " 调用任意后台接口\n");
        sb.append("常用路径：GET /api/v1/system/users（用户列表）, GET /api/v1/system/roles（角色列表）, "
                + "GET /api/v1/system/logs/login（登录日志）, GET /api/v1/ai/usage-logs（AI用量）, "
                + "GET /api/v1/ai/models（模型列表）, GET /api/v1/ai/knowledge-bases（知识库列表）。\n");
        sb.append("输出工具标记后停止。系统会执行并把结果反馈给你，你再用自然语言回复用户。\n");
        return sb.toString();
    }

    /**
     * If the model's reply starts with a {@code [TOOL:name {json}]} marker, execute the tool and re-call the LLM
     * with the tool result appended. Returns the final natural-language reply. Max 2 hops to avoid loops.
     */
    String maybeExecuteTool(
            String reply,
            LoadedChat loaded,
            String userContent,
            List<AiChatAttachment> attachments,
            String systemPrompt) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[TOOL:([a-z_]+)\\s*([^\\]]*)\\]")
                .matcher(reply == null ? "" : reply.trim());
        if (!m.find()) {
            return reply;
        }
        String tool = m.group(1);
        String argsJson = m.group(2).trim();

        // Write operations require user confirmation first: emit a confirmation marker instead of executing.
        if (isWriteTool(tool, argsJson)) {
            return "[CONFIRM_REQUIRED:" + tool + ":" + argsJson + "]";
        }

        String toolResult;
        try {
            toolResult = dispatchTool(tool, argsJson);
        } catch (Exception e) {
            toolResult = "工具执行失败: " + e.getMessage();
        }
        // Re-call LLM with tool result as an additional user turn
        String followUp =
                "用户原始请求: " + userContent + "\n\n工具 [" + tool + "] 执行结果:\n" + toolResult + "\n\n请根据工具结果用自然语言回复用户。";
        LlmChatResult finalResult = llmGateway.chatWithUsage(LlmGatewaySupport.toCommand(
                loaded.provider(),
                loaded.model(),
                loaded.history(),
                followUp,
                attachments,
                chatProperties.publicBaseUrl(),
                systemPrompt));
        recordUsage(loaded, finalResult, "chat", loaded.sessionId());
        return finalResult.content();
    }

    /** A tool that mutates data must be confirmed by the user before execution. */
    private boolean isWriteTool(String tool, String argsJson) {
        if ("create_user".equals(tool)) return true;
        if ("api_call".equals(tool)) {
            try {
                var node = readJson(argsJson);
                String method = node.path("method").asText("GET").toUpperCase();
                return !"GET".equals(method);
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }

    /** Execute a local tool against the DB. MCP tools would go here too; for now we ship the built-ins. */
    String dispatchTool(String tool, String argsJson) {
        return switch (tool) {
            case "list_users" -> {
                var rows = jdbc.queryForList(
                        "SELECT username, nickname, enabled, created_at FROM sys_user WHERE deleted=0 ORDER BY id DESC"
                                + " LIMIT 20");
                yield rows.isEmpty() ? "(没有用户)" : rows.toString();
            }
            case "create_user" -> {
                var args = readJson(argsJson);
                String username = args.path("username").asText("");
                String display = args.path("displayName").asText(username);
                if (username.isBlank()) yield "缺少 username 参数";
                long id = dev.strata.common.core.util.IdGenerator.next();
                jdbc.update(
                        "INSERT INTO sys_user (id, username, nickname, password_hash, enabled, created_at) VALUES (?,"
                                + " ?, ?, '!disabled', 1, NOW())",
                        id,
                        username,
                        display);
                yield "已创建用户 " + username + " (id=" + id + ")，初始密码未设置（管理员可在用户管理页重置）";
            }
            case "list_orders" -> {
                try {
                    var rows = jdbc.queryForList(
                            "SELECT id, player_id, direction, asset_code, amount_minor, status, created_at "
                                    + "FROM casino_payment_intent ORDER BY id DESC LIMIT 10");
                    yield rows.isEmpty() ? "(没有充值记录)" : rows.toString();
                } catch (Exception e) {
                    yield "订单表不可用: " + e.getMessage();
                }
            }
            case "api_call" -> dispatchApiCall(argsJson);
            default -> "未知工具: " + tool;
        };
    }

    /** Generic in-process API call: forwards method+path+body to the local backend with the caller's JWT. */
    private String dispatchApiCall(String argsJson) {
        var args = readJson(argsJson);
        String method = args.path("method").asText("GET").toUpperCase();
        String path = args.path("path").asText("");
        // Tolerate LLM guesses: /v1/... → /api/v1/..., /users → /api/v1/system/users, etc.
        if (path.startsWith("/v1/")) path = "/api" + path;
        else if (path.startsWith("/v2/")) path = "/api" + path;
        else if (!path.startsWith("/api/")) path = "/api/v1/" + path.replaceFirst("^/", "");
        var bodyNode = args.path("body");
        String body = bodyNode.isMissingNode() || bodyNode.isNull() ? null : bodyNode.toString();
        try {
            String baseUrl = chatProperties.publicBaseUrl();
            if (baseUrl == null || baseUrl.isBlank()) baseUrl = "http://localhost:8080";
            var client = org.springframework.web.client.RestClient.builder()
                    .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory())
                    .baseUrl(baseUrl)
                    .build();
            var spec = client.method(org.springframework.http.HttpMethod.valueOf(method))
                    .uri(path);
            // Forward the caller's Authorization header so permission checks apply.
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication();
            if (auth != null && auth.getCredentials() instanceof String token && !token.isBlank()) {
                spec = spec.headers(h -> h.setBearerAuth(token));
            }
            if (body != null && !"GET".equals(method) && !"DELETE".equals(method)) {
                spec = spec.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body(body);
            }
            String resp = spec.retrieve().body(String.class);
            return resp == null ? "(空响应)" : resp.length() > 2000 ? resp.substring(0, 2000) + "…(截断)" : resp;
        } catch (Exception e) {
            return "调用失败: " + e.getMessage();
        }
    }

    private static tools.jackson.databind.JsonNode readJson(String json) {
        try {
            return new tools.jackson.databind.ObjectMapper().readTree(json == null || json.isBlank() ? "{}" : json);
        } catch (Exception e) {
            return new tools.jackson.databind.ObjectMapper().createObjectNode();
        }
    }

    /** Parse "[CONFIRM_APPROVED]|<tool>|<json>" — tolerates trailing newlines/duplicates. Returns [tool, argsJson] or null. */
    static String[] parseApproval(String content) {
        if (content == null || !content.startsWith("[CONFIRM_APPROVED]")) return null;
        int firstPipe = content.indexOf('|');
        int secondPipe = content.indexOf('|', firstPipe + 1);
        if (firstPipe <= 0 || secondPipe <= 0) return null;
        String tool = content.substring(firstPipe + 1, secondPipe).trim();
        String argsRaw = content.substring(secondPipe + 1).trim();
        // Cut off at the first complete JSON object (brace-matching).
        int depth = 0;
        int jsonEnd = -1;
        for (int i = 0; i < argsRaw.length(); i++) {
            char c = argsRaw.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    jsonEnd = i;
                    break;
                }
            }
        }
        String argsJson = jsonEnd > 0 ? argsRaw.substring(0, jsonEnd + 1) : argsRaw;
        return new String[] {tool, argsJson};
    }

    /** Record a chat LLM call in ai_llm_usage; failures are logged and swallowed so they never break a chat turn. */
    void recordUsage(LoadedChat loaded, LlmChatResult result, String action, Long sessionId) {
        try {
            jdbc.update(
                    "INSERT INTO ai_llm_usage (id, provider, model, session_id, prompt_tokens, completion_tokens,"
                            + " total_tokens, cost_minor, action, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?)",
                    dev.strata.common.core.util.IdGenerator.next(),
                    loaded.provider().getCode(),
                    loaded.model().getCode(),
                    String.valueOf(sessionId),
                    result.promptTokens() == null ? 0 : result.promptTokens(),
                    result.completionTokens() == null ? 0 : result.completionTokens(),
                    result.totalTokens() == null ? 0 : result.totalTokens(),
                    action,
                    Timestamp.from(java.time.Instant.now()));
        } catch (Exception ex) {
            log.warn("Failed to record LLM usage: {}", ex.getMessage());
        }
    }
}
