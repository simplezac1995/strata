// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RbacMetrics}.
 *
 * <p>Verifies that all counter metrics are registered and increment correctly.
 */
class RbacMetricsTest {

    private SimpleMeterRegistry registry;
    private RbacMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new RbacMetrics(registry);
    }

    @Test
    void recordLoginSuccessIncrementsCounter() {
        metrics.recordLoginSuccess();
        metrics.recordLoginSuccess();
        assertThat(registry.counter("strata.system.rbac.auth.login.success").count())
                .isEqualTo(2.0);
    }

    @Test
    void recordLoginFailureIncrementsCounter() {
        metrics.recordLoginFailure();
        assertThat(registry.counter("strata.system.rbac.auth.login.failure").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordUserCreatedIncrementsCounter() {
        metrics.recordUserCreated();
        assertThat(registry.counter("strata.system.rbac.user.created").count()).isEqualTo(1.0);
    }

    @Test
    void recordUserUpdatedIncrementsCounter() {
        metrics.recordUserUpdated();
        assertThat(registry.counter("strata.system.rbac.user.updated").count()).isEqualTo(1.0);
    }

    @Test
    void recordUserDeletedIncrementsCounter() {
        metrics.recordUserDeleted();
        assertThat(registry.counter("strata.system.rbac.user.deleted").count()).isEqualTo(1.0);
    }

    @Test
    void recordRoleCreatedIncrementsCounter() {
        metrics.recordRoleCreated();
        assertThat(registry.counter("strata.system.rbac.role.created").count()).isEqualTo(1.0);
    }

    @Test
    void recordRoleUpdatedIncrementsCounter() {
        metrics.recordRoleUpdated();
        assertThat(registry.counter("strata.system.rbac.role.updated").count()).isEqualTo(1.0);
    }

    @Test
    void recordRoleDeletedIncrementsCounter() {
        metrics.recordRoleDeleted();
        assertThat(registry.counter("strata.system.rbac.role.deleted").count()).isEqualTo(1.0);
    }

    @Test
    void allCountersStartAtZero() {
        assertThat(registry.counter("strata.system.rbac.auth.login.success").count())
                .isZero();
        assertThat(registry.counter("strata.system.rbac.auth.login.failure").count())
                .isZero();
        assertThat(registry.counter("strata.system.rbac.user.created").count()).isZero();
        assertThat(registry.counter("strata.system.rbac.user.updated").count()).isZero();
        assertThat(registry.counter("strata.system.rbac.user.deleted").count()).isZero();
        assertThat(registry.counter("strata.system.rbac.role.created").count()).isZero();
        assertThat(registry.counter("strata.system.rbac.role.updated").count()).isZero();
        assertThat(registry.counter("strata.system.rbac.role.deleted").count()).isZero();
    }
}
