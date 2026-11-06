// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.workflow.flowable;

import dev.strata.plugin.api.workflow.WorkflowEngine;
import java.util.Map;
import java.util.Optional;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;

public class FlowableWorkflowEngine implements WorkflowEngine {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;

    public FlowableWorkflowEngine(RuntimeService runtimeService, HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String startProcess(String definitionKey, Map<String, Object> variables) {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(definitionKey, variables);
        return instance.getId();
    }

    @Override
    public Optional<String> pollStatus(String processInstanceId) {
        ProcessInstance running = runtimeService
                .createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (running != null) {
            return Optional.of("RUNNING");
        }
        HistoricProcessInstance historic = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (historic == null) {
            return Optional.empty();
        }
        if (historic.getEndTime() == null) {
            return Optional.of("RUNNING");
        }
        if (historic.getDeleteReason() != null && !historic.getDeleteReason().isBlank()) {
            return Optional.of("FAILED");
        }
        return Optional.of("COMPLETED");
    }
}
