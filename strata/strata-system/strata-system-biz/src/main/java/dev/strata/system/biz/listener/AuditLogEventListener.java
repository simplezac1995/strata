// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.listener;

import dev.strata.system.api.event.AuditLogEvent;
import dev.strata.system.ops.biz.service.AuditLogAppService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AuditLogEventListener {

    private final AuditLogAppService auditLogAppService;

    public AuditLogEventListener(AuditLogAppService auditLogAppService) {
        this.auditLogAppService = auditLogAppService;
    }

    @Async("operationLogExecutor")
    @EventListener
    public void onAuditLog(AuditLogEvent event) {
        auditLogAppService.recordEvent(event);
    }
}
