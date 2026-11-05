// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.ai.api.request.AgentNodeUpsertRequest;
import dev.strata.ai.api.request.AgentRunRequest;
import dev.strata.ai.config.api.entity.AiAgent;
import dev.strata.ai.config.api.entity.AiAgentRun;
import dev.strata.ai.config.biz.repository.AiAgentNodeRepository;
import dev.strata.ai.config.biz.repository.AiAgentRepository;
import dev.strata.ai.config.biz.repository.AiAgentRunRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentRunAppService 运行与节点保存")
class AgentRunAppServiceTest {

    @Mock
    private AiAgentRepository agentRepository;

    @Mock
    private AiAgentNodeRepository nodeRepository;

    @Mock
    private AiAgentRunRepository runRepository;

    @Mock
    private AgentDagEngine dagEngine;

    private AgentRunAppService service;

    @BeforeEach
    void setUp() {
        service = new AgentRunAppService(agentRepository, nodeRepository, runRepository, dagEngine, new ObjectMapper());
    }

    @Test
    @DisplayName("Agent 不存在抛 NOT_FOUND")
    void unknown_agent_rejected() {
        when(agentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.run(404L, new AgentRunRequest("in")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("run 记录运行结果并持久化为 completed")
    void run_persists_completed_run() {
        AiAgent agent = new AiAgent();
        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));
        when(nodeRepository.findByAgentIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(dagEngine.run(any(), eq("in")))
                .thenReturn(new AgentDagEngine.EngineResult("final", Map.of("input", "in"), List.of()));
        when(runRepository.save(any(AiAgentRun.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.run(1L, new AgentRunRequest("in"));

        assertThat(resp.finalOutput()).isEqualTo("final");
        assertThat(resp.runId()).isNotNull();
    }

    @Test
    @DisplayName("saveNodes 先删后插并返回节点数")
    void save_nodes_replaces_graph() {
        AiAgent agent = new AiAgent();
        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));
        AgentNodeUpsertRequest.NodeDef nodeDef = new AgentNodeUpsertRequest.NodeDef(
                "start", "llm", "n", Map.of("prompt", "p"), BigDecimal.ONE, BigDecimal.valueOf(2), List.of());
        AgentNodeUpsertRequest req = new AgentNodeUpsertRequest(List.of(nodeDef));

        int count = service.saveNodes(1L, req);

        assertThat(count).isEqualTo(1);
        verify(nodeRepository).deleteByAgentId(1L);
        verify(nodeRepository).save(any());
    }
}
