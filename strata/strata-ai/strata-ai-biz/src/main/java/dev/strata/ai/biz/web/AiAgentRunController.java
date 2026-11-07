// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.web;

import dev.strata.ai.api.dto.AgentRunResponse;
import dev.strata.ai.api.request.AgentNodeUpsertRequest;
import dev.strata.ai.api.request.AgentRunRequest;
import dev.strata.ai.biz.agent.AgentRunAppService;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DAG runtime for agents: execute an agent's node graph and persist/replace its node definition. Sits under
 * the existing {@code /api/v1/ai/agents} tree alongside the CRUD controller.
 */
@Validated
@RestController
@RequestMapping("/api/v1/ai/agents")
public class AiAgentRunController {

    private final AgentRunAppService agentRunAppService;

    public AiAgentRunController(AgentRunAppService agentRunAppService) {
        this.agentRunAppService = agentRunAppService;
    }

    @PostMapping("/{id}/run")
    @PreAuthorize(Permissions.AI_AGENT_LIST)
    public Result<AgentRunResponse> run(@PathVariable Long id, @Valid @RequestBody AgentRunRequest request) {
        return Result.ok(agentRunAppService.run(id, request));
    }

    @PutMapping("/{id}/nodes")
    @PreAuthorize(Permissions.AI_AGENT_UPDATE)
    public Result<Integer> saveNodes(@PathVariable Long id, @Valid @RequestBody AgentNodeUpsertRequest request) {
        return Result.ok(agentRunAppService.saveNodes(id, request));
    }
}
