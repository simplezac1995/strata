// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.workflow.flowable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "varargs"})
class FlowableWorkflowEngineTest {

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private HistoryService historyService;

    private FlowableWorkflowEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FlowableWorkflowEngine(runtimeService, historyService);
    }

    @Test
    void isAvailableReturnsTrue() {
        assertTrue(engine.isAvailable());
    }

    @Test
    void startProcessReturnsInstanceId() {
        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getId()).thenReturn("pi-1");
        when(runtimeService.startProcessInstanceByKey(eq("strataDemo"), any(Map.class)))
                .thenReturn(instance);

        String id = engine.startProcess("strataDemo", Map.of("key", "value"));

        assertEquals("pi-1", id);
    }

    @Test
    void pollStatusReturnsRunningWhenActiveInstanceExists() {
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        ProcessInstance running = mock(ProcessInstance.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceId("pi-1")).thenReturn(query);
        when(query.singleResult()).thenReturn(running);

        assertEquals(Optional.of("RUNNING"), engine.pollStatus("pi-1"));
    }

    @Test
    void pollStatusReturnsCompletedWhenHistoricEnded() {
        ProcessInstanceQuery runtimeQuery = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(runtimeQuery);
        when(runtimeQuery.processInstanceId("pi-2")).thenReturn(runtimeQuery);
        when(runtimeQuery.singleResult()).thenReturn(null);

        HistoricProcessInstanceQuery historicQuery = mock(HistoricProcessInstanceQuery.class);
        HistoricProcessInstance historic = mock(HistoricProcessInstance.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicQuery);
        when(historicQuery.processInstanceId("pi-2")).thenReturn(historicQuery);
        when(historicQuery.singleResult()).thenReturn(historic);
        when(historic.getEndTime()).thenReturn(new Date());
        when(historic.getDeleteReason()).thenReturn(null);

        assertEquals(Optional.of("COMPLETED"), engine.pollStatus("pi-2"));
    }

    @Test
    void pollStatusReturnsFailedWhenHistoricHasDeleteReason() {
        ProcessInstanceQuery runtimeQuery = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(runtimeQuery);
        when(runtimeQuery.processInstanceId("pi-3")).thenReturn(runtimeQuery);
        when(runtimeQuery.singleResult()).thenReturn(null);

        HistoricProcessInstanceQuery historicQuery = mock(HistoricProcessInstanceQuery.class);
        HistoricProcessInstance historic = mock(HistoricProcessInstance.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicQuery);
        when(historicQuery.processInstanceId("pi-3")).thenReturn(historicQuery);
        when(historicQuery.singleResult()).thenReturn(historic);
        when(historic.getEndTime()).thenReturn(new Date());
        when(historic.getDeleteReason()).thenReturn("cancelled");

        assertEquals(Optional.of("FAILED"), engine.pollStatus("pi-3"));
    }

    @Test
    void pollStatusReturnsEmptyWhenUnknownInstance() {
        ProcessInstanceQuery runtimeQuery = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(runtimeQuery);
        when(runtimeQuery.processInstanceId("missing")).thenReturn(runtimeQuery);
        when(runtimeQuery.singleResult()).thenReturn(null);

        HistoricProcessInstanceQuery historicQuery = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicQuery);
        when(historicQuery.processInstanceId("missing")).thenReturn(historicQuery);
        when(historicQuery.singleResult()).thenReturn(null);

        assertEquals(Optional.empty(), engine.pollStatus("missing"));
    }
}
