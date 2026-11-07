// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.agent;

import dev.strata.ai.biz.service.AiUsageRecorder;
import dev.strata.ai.config.api.entity.AiAgentNode;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.ai.config.biz.repository.AiModelRepository;
import dev.strata.ai.config.biz.repository.AiProviderRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.plugin.api.llm.LlmChatCommand;
import dev.strata.plugin.api.llm.LlmChatMessage;
import dev.strata.plugin.api.llm.LlmChatResult;
import dev.strata.plugin.api.llm.LlmGateway;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Executes an agent's node graph as a straight-line DAG walk.
 *
 * <p>Start = the node keyed {@code start}, otherwise the node no other node points to. Edges are the node's
 * {@code nextNodeKeys} list (first edge followed). {@code llm} nodes call the gateway, feeding the previous
 * node's output into the prompt and storing the reply in {@code context}; {@code branch} nodes pick the next
 * edge by comparing a context variable against configured branches; {@code tool} nodes are skipped this
 * iteration. A 10-step cap guards against cycles.
 */
@Component
public class AgentDagEngine {

    private static final Logger log = LoggerFactory.getLogger(AgentDagEngine.class);

    /** Safety cap so a malformed graph cannot loop forever. */
    static final int MAX_STEPS = 10;

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final LlmGateway llmGateway;
    private final AiProviderRepository providerRepository;
    private final AiModelRepository modelRepository;
    private final ObjectMapper objectMapper;
    private final AiUsageRecorder usageRecorder;

    public AgentDagEngine(
            LlmGateway llmGateway,
            AiProviderRepository providerRepository,
            AiModelRepository modelRepository,
            ObjectMapper objectMapper,
            AiUsageRecorder usageRecorder) {
        this.llmGateway = llmGateway;
        this.providerRepository = providerRepository;
        this.modelRepository = modelRepository;
        this.objectMapper = objectMapper;
        this.usageRecorder = usageRecorder;
    }

    /** Outcome of one DAG walk. */
    public record EngineResult(String finalOutput, Map<String, Object> context, List<StepTrace> steps) {}

    /** One executed vertex. */
    public record StepTrace(String nodeKey, String status, String output) {}

    /**
     * Walk the graph starting from {@code input}. Nodes not present in {@code nodes} are skipped; an empty
     * graph yields the input as the final output.
     */
    public EngineResult run(List<AiAgentNode> nodes, String input) {
        Map<String, AiAgentNode> byKey = new LinkedHashMap<>();
        for (AiAgentNode node : nodes) {
            byKey.put(node.getNodeKey(), node);
        }
        Map<String, Object> context = new HashMap<>();
        context.put("input", input == null ? "" : input);
        List<StepTrace> steps = new ArrayList<>();
        String finalOutput = input == null ? "" : input;

        AiAgentNode current = startNode(nodes);
        String previousOutput = input == null ? "" : input;
        for (int step = 0; step < MAX_STEPS && current != null; step++) {
            StepOutcome outcome = executeNode(current, previousOutput, context);
            steps.add(new StepTrace(current.getNodeKey(), outcome.status(), outcome.output()));
            if ("llm".equals(current.getNodeType())) {
                finalOutput = outcome.output();
                previousOutput = outcome.output();
            }
            String nextKey = nextNodeKey(current, context);
            current = nextKey == null ? null : byKey.get(nextKey);
        }
        return new EngineResult(finalOutput, context, steps);
    }

    /** Pick the start vertex: explicit {@code start}, else the root (node nobody points to). */
    private AiAgentNode startNode(List<AiAgentNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        Set<String> targets = new HashSet<>();
        for (AiAgentNode node : nodes) {
            targets.addAll(parseNextKeys(node.getNextNodeKeys()));
        }
        for (AiAgentNode node : nodes) {
            if ("start".equals(node.getNodeKey())) {
                return node;
            }
        }
        for (AiAgentNode node : nodes) {
            if (!targets.contains(node.getNodeKey())) {
                return node;
            }
        }
        return nodes.get(0);
    }

    private StepOutcome executeNode(AiAgentNode node, String previousOutput, Map<String, Object> context) {
        String type = node.getNodeType() == null ? "" : node.getNodeType().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "llm" -> {
                String prompt = llmPrompt(node, context);
                AiProvider provider = resolveProvider();
                AiModel model = resolveModel(provider);
                LlmChatResult result = llmGateway.chatWithUsage(new LlmChatCommand(
                        provider.getBaseUrl(),
                        provider.getApiKey(),
                        model.getCode(),
                        List.of(new LlmChatMessage("system", prompt)),
                        previousOutput));
                String reply = result.content();
                usageRecorder.record(provider.getCode(), model.getCode(), null, "agent", result);
                context.put(node.getNodeKey(), reply);
                yield new StepOutcome("completed", reply);
            }
            case "branch" -> new StepOutcome("completed", previousOutput);
            default -> new StepOutcome("skipped", previousOutput);
        };
    }

    /** Resolve the outgoing edge: branch nodes inspect a context variable; others follow the first edge. */
    private String nextNodeKey(AiAgentNode node, Map<String, Object> context) {
        if ("branch".equalsIgnoreCase(node.getNodeType())) {
            return branchNext(node, context);
        }
        List<String> next = parseNextKeys(node.getNextNodeKeys());
        return next.isEmpty() ? null : next.get(0);
    }

    @SuppressWarnings("unchecked")
    private String branchNext(AiAgentNode node, Map<String, Object> context) {
        Map<String, Object> config = parseConfig(node.getConfig());
        String variable = (String) config.get("variable");
        Object actual = variable == null ? null : context.get(variable);
        Map<String, Object> branches =
                config.get("branches") instanceof Map<?, ?> raw ? (Map<String, Object>) raw : Map.of();
        if (actual != null && branches.containsKey(String.valueOf(actual))) {
            Object chosen = branches.get(String.valueOf(actual));
            if (chosen instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        Object fallback = config.get("default");
        return fallback instanceof String s && !s.isBlank() ? s : null;
    }

    private String llmPrompt(AiAgentNode node, Map<String, Object> context) {
        Map<String, Object> config = parseConfig(node.getConfig());
        Object template = config.getOrDefault("prompt", config.getOrDefault("template", node.getName()));
        String instruction = template == null ? "Continue the workflow." : String.valueOf(template);
        StringBuilder sb = new StringBuilder(instruction);
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            sb.append("\n\n[").append(entry.getKey()).append("]\n").append(entry.getValue());
        }
        return sb.toString();
    }

    private List<String> parseNextKeys(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (Exception ex) {
            log.warn("Ignoring malformed next_node_keys JSON: {}", ex.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> parseConfig(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP);
        } catch (Exception ex) {
            log.warn("Ignoring malformed node config JSON: {}", ex.getMessage());
            return Map.of();
        }
    }

    private AiProvider resolveProvider() {
        return providerRepository
                .findFirstByEnabledTrueOrderByIdAsc()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "No enabled AI provider configured"));
    }

    private AiModel resolveModel(AiProvider provider) {
        return modelRepository
                .findFirstByEnabledTrueAndProviderIdOrderByIdAsc(provider.getId())
                .or(() -> modelRepository.findFirstByEnabledTrueOrderByIdAsc())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "No enabled AI model configured"));
    }

    private record StepOutcome(String status, String output) {}
}
