// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.listener;

import dev.strata.system.api.event.OperationLogEvent;
import dev.strata.system.ops.api.entity.OperationLog;
import dev.strata.system.ops.biz.repository.OperationLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OperationLogEventListener {

    private final OperationLogRepository operationLogRepository;

    public OperationLogEventListener(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    @Async("operationLogExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOperationLog(OperationLogEvent event) {
        OperationLog log = new OperationLog();
        log.setModuleName(event.module());
        log.setActionName(event.action());
        log.setOperatorId(event.operatorId());
        log.setOperatorName(event.operatorName());
        log.setDetail(event.detail());
        operationLogRepository.save(log);
    }
}
