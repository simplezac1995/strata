// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.aspect;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.system.api.event.AuditLogEvent;
import dev.strata.system.api.event.OperationLogEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final ApplicationEventPublisher eventPublisher;

    public OperationLogAspect(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        Long operatorId = null;
        String operatorName = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtPrincipal principal) {
            operatorId = principal.userId();
            operatorName = principal.username();
        }
        String detail = joinPoint.getSignature().toShortString();

        String ipAddress = null;
        String userAgent = null;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ipAddress = request.getRemoteAddr();
            userAgent = request.getHeader("User-Agent");
            if (userAgent != null && userAgent.length() > 256) {
                userAgent = userAgent.substring(0, 256);
            }
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable failure) {
            String failureDetail = detail + " failed (" + failure.getClass().getSimpleName() + ")";
            try {
                publishAuditEvent(operationLog, operatorId, operatorName, ipAddress, userAgent, false, failureDetail);
            } catch (RuntimeException auditFailure) {
                // Do not replace the original business failure with an audit-event publication failure.
                log.error("Could not publish failed-operation audit event for {}", detail, auditFailure);
            }
            throw failure;
        }

        eventPublisher.publishEvent(
                new OperationLogEvent(operationLog.module(), operationLog.action(), operatorId, operatorName, detail));
        publishAuditEvent(operationLog, operatorId, operatorName, ipAddress, userAgent, true, detail);
        return result;
    }

    private void publishAuditEvent(
            OperationLog operationLog,
            Long operatorId,
            String operatorName,
            String ipAddress,
            String userAgent,
            boolean success,
            String detail) {
        AuditLogEvent event = new AuditLogEvent(
                AuditLogSupport.eventTypeForModule(operationLog.module()),
                operationLog.module(),
                operationLog.action(),
                AuditLogSupport.resourceTypeForModule(operationLog.module()),
                null,
                operatorId,
                operatorName,
                ipAddress,
                userAgent,
                success,
                detail);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eventPublisher.publishEvent(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                AuditLogEvent completedEvent = event;
                if (status != STATUS_COMMITTED) {
                    String completionDetail =
                            event.success() ? event.detail() + " transaction rolled back" : event.detail();
                    completedEvent = new AuditLogEvent(
                            event.eventType(),
                            event.module(),
                            event.action(),
                            event.resourceType(),
                            event.resourceId(),
                            event.operatorId(),
                            event.operatorName(),
                            event.ipAddress(),
                            event.userAgent(),
                            false,
                            completionDetail);
                }
                try {
                    eventPublisher.publishEvent(completedEvent);
                } catch (RuntimeException auditFailure) {
                    log.error("Could not publish completed-operation audit event for {}", detail, auditFailure);
                }
            }
        });
    }
}
