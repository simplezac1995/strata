// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.agent;

import dev.strata.ai.api.dto.AgentRunResponse;
import dev.strata.ai.api.request.AgentNodeUpsertRequest;
import dev.strata.ai.api.request.AgentRunRequest;
import dev.strata.ai.config.api.entity.AiAgent;
import dev.strata.ai.config.api.entity.AiAgentNode;
import dev.strata.ai.config.api.entity.AiAgentRun;
import dev.strata.ai.config.biz.repository.AiAgentNodeRepository;
import dev.strata.ai.config.biz.repository.AiAgentRepository;
import dev.strata.ai.config.biz.repository.AiAgentRunRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Drives {@link AgentDagEngine} for the HTTP layer: loads an agent's node graph, records a run row, runs the
 * graph, and persists the outcome. Also owns the node-graph upsert endpoint.
 */
@Service
public class AgentRunAppService {

    private static final Logger log = LoggerFactory.getLogger(AgentRunAppService.class);

    private final AiAgentRepository agentRepository;
    private final AiAgentNodeRepository nodeRepository;
    private final AiAgentRunRepository runRepository;
    private final AgentDagEngine dagEngine;
    private final ObjectMapper objectMapper;

    public AgentRunAppService(
            AiAgentRepository agentRepository,
            AiAgentNodeRepository nodeRepository,
            AiAgentRunRepository runRepository,
            AgentDagEngine dagEngine,
            ObjectMapper objectMapper) {
        this.agentRepository = agentRepository;
        this.nodeRepository = nodeRepository;
        this.runRepository = runRepository;
        this.dagEngine = dagEngine;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AgentRunResponse run(Long agentId, AgentRunRequest request) {
        AiAgent agent = requireAgent(agentId);

        AiAgentRun run = new AiAgentRun();
        run.setAgentId(agentId);
        run.setStatus("running");
        run.setStartedAt(Instant.now());
        runRepository.save(run);

        try {
            List<AiAgentNode> nodes = nodeRepository.findByAgentIdOrderByIdAsc(agentId);
            AgentDagEngine.EngineResult result = dagEngine.run(nodes, request.input());

            run.setStatus("completed");
            run.setContext(toJson(result.context()));
            run.setFinishedAt(Instant.now());
            runRepository.save(run);

            List<AgentRunResponse.Step> steps = result.steps().stream()
                    .map(s -> new AgentRunResponse.Step(s.nodeKey(), s.status(), s.output()))
                    .toList();
            return new AgentRunResponse(String.valueOf(run.getId()), result.finalOutput(), result.context(), steps);
        } catch (RuntimeException ex) {
            log.error("Agent run {} failed for agent {}", run.getId(), agentId, ex);
            run.setStatus("failed");
            run.setError(ex.getMessage());
            run.setFinishedAt(Instant.now());
            runRepository.save(run);
            throw ex;
        }
    }

    /** Replace the node graph of an agent; returns the number of nodes stored. */
    @Transactional
    public int saveNodes(Long agentId, AgentNodeUpsertRequest request) {
        requireAgent(agentId);
        nodeRepository.deleteByAgentId(agentId);
        int count = 0;
        for (AgentNodeUpsertRequest.NodeDef def : request.nodes()) {
            AiAgentNode node = new AiAgentNode();
            node.setAgentId(agentId);
            node.setNodeKey(def.nodeKey());
            node.setNodeType(def.nodeType());
            node.setName(def.name());
            node.setConfig(toJson(def.config()));
            node.setPositionX(def.positionX());
            node.setPositionY(def.positionY());
            node.setNextNodeKeys(toJson(def.nextNodeKeys()));
            nodeRepository.save(node);
            count++;
        }
        return count;
    }

    private AiAgent requireAgent(Long agentId) {
        return agentRepository.findById(agentId).orElseThrow(() -> new NotFoundException("Agent not found"));
    }

    private String toJson(Object value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to serialize agent payload");
        }
    }
}
