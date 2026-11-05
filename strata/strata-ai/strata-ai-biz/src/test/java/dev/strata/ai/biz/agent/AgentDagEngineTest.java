// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import dev.strata.ai.biz.service.AiUsageRecorder;
import dev.strata.ai.config.api.entity.AiAgentNode;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.ai.config.biz.repository.AiModelRepository;
import dev.strata.ai.config.biz.repository.AiProviderRepository;
import dev.strata.plugin.api.llm.LlmChatResult;
import dev.strata.plugin.api.llm.LlmGateway;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AgentDagEngine DAG 执行")
class AgentDagEngineTest {

    @Mock
    private LlmGateway llmGateway;

    @Mock
    private AiProviderRepository providerRepository;

    @Mock
    private AiModelRepository modelRepository;

    @Mock
    private AiUsageRecorder usageRecorder;

    private AgentDagEngine engine;

    @BeforeEach
    void setUp() {
        AiProvider p = new AiProvider();
        p.setId(10L);
        p.setBaseUrl("https://llm");
        p.setApiKey("k");
        when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.of(p));
        AiModel m = new AiModel();
        m.setId(20L);
        m.setProviderId(10L);
        m.setCode("gpt");
        when(modelRepository.findFirstByEnabledTrueAndProviderIdOrderByIdAsc(10L))
                .thenReturn(Optional.of(m));
        engine = new AgentDagEngine(llmGateway, providerRepository, modelRepository, new ObjectMapper(), usageRecorder);
    }

    private AiAgentNode node(String key, String type, String config, String nextKeys) {
        AiAgentNode n = new AiAgentNode();
        n.setNodeKey(key);
        n.setNodeType(type);
        n.setConfig(config);
        n.setNextNodeKeys(nextKeys);
        return n;
    }

    private static LlmChatResult result(String content) {
        return new LlmChatResult(content, 1, 1, 2, "gpt", "stop");
    }

    @Test
    @DisplayName("空图直接返回输入")
    void empty_graph_returns_input() {
        AgentDagEngine.EngineResult r = engine.run(List.of(), "hello");
        assertThat(r.finalOutput()).isEqualTo("hello");
        assertThat(r.steps()).isEmpty();
    }

    @Test
    @DisplayName("线性 llm 链：上一节点输出拼进下一节点")
    void linear_llm_chain_chains_outputs() {
        when(llmGateway.chatWithUsage(any())).thenReturn(result("step1")).thenReturn(result("step2"));
        AiAgentNode a = node("start", "llm", "{\"prompt\":\"p1\"}", "[\"b\"]");
        AiAgentNode b = node("b", "llm", "{\"prompt\":\"p2\"}", "[]");

        AgentDagEngine.EngineResult r = engine.run(List.of(a, b), "input");

        assertThat(r.steps()).extracting(AgentDagEngine.StepTrace::nodeKey).containsExactly("start", "b");
        assertThat(r.finalOutput()).isEqualTo("step2");
        assertThat(r.context().get("start")).isEqualTo("step1");
    }

    @Test
    @DisplayName("分支节点按上下文变量选择下一个节点")
    void branch_selects_next_by_context() {
        AiAgentNode start = node("start", "llm", "{\"prompt\":\"classify\"}", "[\"decide\"]");
        AiAgentNode decide = node(
                "decide",
                "branch",
                "{\"variable\":\"start\",\"branches\":{\"ready\":\"done\"},\"default\":\"fallback\"}",
                "[]");
        AiAgentNode done = node("done", "llm", "{\"prompt\":\"final\"}", "[]");
        when(llmGateway.chatWithUsage(any())).thenReturn(result("ready")).thenReturn(result("final answer"));

        AgentDagEngine.EngineResult r = engine.run(List.of(start, decide, done), "q");

        assertThat(r.steps()).extracting(AgentDagEngine.StepTrace::nodeKey).containsExactly("start", "decide", "done");
        assertThat(r.finalOutput()).isEqualTo("final answer");
    }

    @Test
    @DisplayName("环形图在 10 步上限处截断")
    void cycle_is_capped() {
        when(llmGateway.chatWithUsage(any())).thenReturn(result("x"));
        AiAgentNode loop = node("start", "llm", "{\"prompt\":\"p\"}", "[\"start\"]");

        AgentDagEngine.EngineResult r = engine.run(List.of(loop), "q");

        assertThat(r.steps()).hasSize(AgentDagEngine.MAX_STEPS);
    }
}
