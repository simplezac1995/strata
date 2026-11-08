// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Business metrics for the RBAC subdomain.
 *
 * <p>Tracks authentication outcomes and user management operations. All counters use the
 * {@code strata.system.rbac.*} namespace.
 */
@Component
public class RbacMetrics {

    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter userCreatedCounter;
    private final Counter userUpdatedCounter;
    private final Counter userDeletedCounter;
    private final Counter roleCreatedCounter;
    private final Counter roleUpdatedCounter;
    private final Counter roleDeletedCounter;

    public RbacMetrics(MeterRegistry registry) {
        this.loginSuccessCounter = Counter.builder("strata.system.rbac.auth.login.success")
                .description("Successful login attempts")
                .register(registry);
        this.loginFailureCounter = Counter.builder("strata.system.rbac.auth.login.failure")
                .description("Failed login attempts")
                .register(registry);
        this.userCreatedCounter = Counter.builder("strata.system.rbac.user.created")
                .description("Users created")
                .register(registry);
        this.userUpdatedCounter = Counter.builder("strata.system.rbac.user.updated")
                .description("Users updated")
                .register(registry);
        this.userDeletedCounter = Counter.builder("strata.system.rbac.user.deleted")
                .description("Users deleted")
                .register(registry);
        this.roleCreatedCounter = Counter.builder("strata.system.rbac.role.created")
                .description("Roles created")
                .register(registry);
        this.roleUpdatedCounter = Counter.builder("strata.system.rbac.role.updated")
                .description("Roles updated")
                .register(registry);
        this.roleDeletedCounter = Counter.builder("strata.system.rbac.role.deleted")
                .description("Roles deleted")
                .register(registry);
    }

    public void recordLoginSuccess() {
        loginSuccessCounter.increment();
    }

    public void recordLoginFailure() {
        loginFailureCounter.increment();
    }

    public void recordUserCreated() {
        userCreatedCounter.increment();
    }

    public void recordUserUpdated() {
        userUpdatedCounter.increment();
    }

    public void recordUserDeleted() {
        userDeletedCounter.increment();
    }

    public void recordRoleCreated() {
        roleCreatedCounter.increment();
    }

    public void recordRoleUpdated() {
        roleUpdatedCounter.increment();
    }

    public void recordRoleDeleted() {
        roleDeletedCounter.increment();
    }
}
