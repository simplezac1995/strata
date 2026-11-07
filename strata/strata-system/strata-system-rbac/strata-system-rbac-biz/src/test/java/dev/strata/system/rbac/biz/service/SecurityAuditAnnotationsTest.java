// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.system.api.request.ChangePasswordRequest;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * P1-8 audit coverage: the sensitive auth operations must carry the declarative {@link OperationLog}
 * annotation so the existing aspect lands an audit row. These are reflection guards (the Spring AOP aspect
 * itself lives in system-biz and is exercised there); they fail if a future refactor drops the annotation or
 * renames the action.
 */
class SecurityAuditAnnotationsTest {

    private static OperationLog annotationOn(Class<?> owner, String methodName, Class<?>... paramTypes)
            throws NoSuchMethodException {
        Method m = owner.getDeclaredMethod(methodName, paramTypes);
        OperationLog log = m.getAnnotation(OperationLog.class);
        assertThat(log)
                .as("@OperationLog missing on %s#%s", owner.getSimpleName(), methodName)
                .isNotNull();
        return log;
    }

    @Test
    void changePassword_isAudited() throws NoSuchMethodException {
        OperationLog log =
                annotationOn(AuthAppService.class, "changePassword", Long.class, ChangePasswordRequest.class);
        assertThat(log.module()).isEqualTo("auth:password");
        assertThat(log.action()).isEqualTo("change");
    }

    @Test
    void totpEnable_isAudited() throws NoSuchMethodException {
        OperationLog log = annotationOn(TotpAppService.class, "confirmEnrollment", Long.class, String.class);
        assertThat(log.module()).isEqualTo("auth:mfa");
        assertThat(log.action()).isEqualTo("enable");
    }

    @Test
    void totpDisable_isAudited() throws NoSuchMethodException {
        OperationLog log = annotationOn(TotpAppService.class, "disable", Long.class, String.class);
        assertThat(log.module()).isEqualTo("auth:mfa");
        assertThat(log.action()).isEqualTo("disable");
    }

    @Test
    void totpReset_isAudited() throws NoSuchMethodException {
        OperationLog log = annotationOn(TotpAppService.class, "reset", Long.class);
        assertThat(log.module()).isEqualTo("auth:mfa");
        assertThat(log.action()).isEqualTo("reset");
    }
}
