// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.datascope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DataScopeContextTest {

    @AfterEach
    void clearContext() {
        DataScopeContext.clear();
    }

    @Test
    void allDataFactory() {
        DataScopeContext ctx = DataScopeContext.allData(1L, "dept_id", "created_by");

        assertThat(ctx.getScopeType()).isEqualTo(DataScopeType.ALL);
        assertThat(ctx.isAllData()).isTrue();
        assertThat(ctx.isDeptFiltered()).isFalse();
        assertThat(ctx.isSelfFiltered()).isFalse();
        assertThat(ctx.getUserId()).isEqualTo(1L);
        assertThat(ctx.getDeptField()).isEqualTo("dept_id");
        assertThat(ctx.getUserField()).isEqualTo("created_by");
        assertThat(ctx.getDeptIds()).isEmpty();
    }

    @Test
    void deptScopedCarriesImmutableDeptSet() {
        DataScopeContext ctx = DataScopeContext.deptScoped(
                DataScopeType.DEPT_AND_CHILDREN, 1L, 5L, Set.of(5L, 6L), "dept_id", "created_by");

        assertThat(ctx.isDeptFiltered()).isTrue();
        assertThat(ctx.isSelfFiltered()).isFalse();
        assertThat(ctx.getDeptId()).isEqualTo(5L);
        assertThat(ctx.getDeptIds()).containsExactlyInAnyOrder(5L, 6L);
        assertThatThrownBy(() -> ctx.getDeptIds().add(7L)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deptScopedNullSetBecomesEmpty() {
        DataScopeContext ctx =
                DataScopeContext.deptScoped(DataScopeType.DEPT_ONLY, 1L, null, null, "dept_id", "created_by");

        assertThat(ctx.getDeptIds()).isNotNull().isEmpty();
    }

    @Test
    void selfOnlyFactory() {
        DataScopeContext ctx = DataScopeContext.selfOnly(7L, "dept_id", "created_by");

        assertThat(ctx.getScopeType()).isEqualTo(DataScopeType.SELF_ONLY);
        assertThat(ctx.isSelfFiltered()).isTrue();
        assertThat(ctx.isDeptFiltered()).isFalse();
        assertThat(ctx.getUserId()).isEqualTo(7L);
    }

    @Test
    void threadLocalSetGetClear() {
        DataScopeContext ctx = DataScopeContext.selfOnly(7L, "dept_id", "created_by");

        DataScopeContext.set(ctx);
        assertThat(DataScopeContext.get()).isSameAs(ctx);

        DataScopeContext.clear();
        assertThat(DataScopeContext.get()).isNull();
    }

    @Test
    void contextIsThreadIsolated() throws InterruptedException {
        DataScopeContext.set(DataScopeContext.allData(1L, "dept_id", "created_by"));

        AtomicReference<DataScopeContext> otherThreadValue = new AtomicReference<>();
        Thread thread = new Thread(() -> otherThreadValue.set(DataScopeContext.get()));
        thread.start();
        thread.join();

        assertThat(otherThreadValue.get()).isNull();
        assertThat(DataScopeContext.get()).isNotNull();
    }
}
