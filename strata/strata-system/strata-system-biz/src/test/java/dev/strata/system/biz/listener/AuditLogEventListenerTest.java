// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.listener;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.system.api.event.AuditLogEvent;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;

class AuditLogEventListenerTest {

    @Test
    void consumes_completed_audit_events_asynchronously() throws NoSuchMethodException {
        Method method = AuditLogEventListener.class.getDeclaredMethod("onAuditLog", AuditLogEvent.class);

        assertThat(method.getAnnotation(EventListener.class)).isNotNull();
        assertThat(method.getAnnotation(org.springframework.scheduling.annotation.Async.class))
                .isNotNull();
    }
}
