// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.system.api.event.AuditLogEvent;
import dev.strata.system.api.event.OperationLogEvent;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;

class OperationLogAspectTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final OperationLogAspect aspect = new OperationLogAspect(eventPublisher);

    @AfterEach
    void clearThreadContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void failed_operation_emits_failure_audit_without_exception_message() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint();
        IllegalArgumentException failure = new IllegalArgumentException("sensitive request body");
        when(joinPoint.proceed()).thenThrow(failure);

        assertThatThrownBy(() -> aspect.around(joinPoint, operationLog())).isSameAs(failure);

        ArgumentCaptor<AuditLogEvent> auditEvent = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(auditEvent.capture());
        assertThat(auditEvent.getValue().success()).isFalse();
        assertThat(auditEvent.getValue().detail())
                .isEqualTo("TestService.review(..) failed (IllegalArgumentException)");
        assertThat(auditEvent.getValue().detail()).doesNotContain("sensitive request body");
    }

    @Test
    void successful_operation_keeps_existing_operation_and_audit_events() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint();
        when(joinPoint.proceed()).thenReturn("done");

        Object result = aspect.around(joinPoint, operationLog());

        assertThat(result).isEqualTo("done");
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(OperationLogEvent.class));
        ArgumentCaptor<AuditLogEvent> auditEvent = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(auditEvent.capture());
        assertThat(auditEvent.getValue().success()).isTrue();
        assertThat(auditEvent.getValue().detail()).isEqualTo("TestService.review(..)");
    }

    @Test
    void successful_method_is_marked_failed_if_enclosing_transaction_rolls_back() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint();
        when(joinPoint.proceed()).thenReturn("done");
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        aspect.around(joinPoint, operationLog());

        verify(eventPublisher, org.mockito.Mockito.never())
                .publishEvent(org.mockito.ArgumentMatchers.any(AuditLogEvent.class));
        TransactionSynchronization[] synchronizations =
                TransactionSynchronizationManager.getSynchronizations().toArray(TransactionSynchronization[]::new);
        synchronizations[0].afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        ArgumentCaptor<AuditLogEvent> auditEvent = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(auditEvent.capture());
        assertThat(auditEvent.getValue().success()).isFalse();
        assertThat(auditEvent.getValue().detail()).endsWith("transaction rolled back");
    }

    private static ProceedingJoinPoint joinPoint() {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.toShortString()).thenReturn("TestService.review(..)");
        when(joinPoint.getSignature()).thenReturn(signature);
        return joinPoint;
    }

    private static OperationLog operationLog() throws NoSuchMethodException {
        Method method = TestService.class.getDeclaredMethod("review", Long.class);
        return method.getAnnotation(OperationLog.class);
    }

    private static final class TestService {
        @OperationLog(module = "compliance:aml", action = "review")
        private void review(Long id) {}
    }
}
