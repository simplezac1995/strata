// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.datascope;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DataScopeTypeTest {

    @Test
    void allMeansNoFiltering() {
        assertThat(DataScopeType.ALL.isAllData()).isTrue();
        assertThat(DataScopeType.ALL.requiresDeptFilter()).isFalse();
        assertThat(DataScopeType.ALL.requiresSelfFilter()).isFalse();
    }

    @Test
    void departmentScopesRequireDeptFilter() {
        Set<DataScopeType> deptScopes =
                Set.of(DataScopeType.DEPT_ONLY, DataScopeType.DEPT_AND_CHILDREN, DataScopeType.CUSTOM);
        for (DataScopeType type : deptScopes) {
            assertThat(type.isAllData()).as("%s 不是全部数据", type).isFalse();
            assertThat(type.requiresDeptFilter()).as("%s 需要部门过滤", type).isTrue();
            assertThat(type.requiresSelfFilter()).as("%s 不需要本人过滤", type).isFalse();
        }
    }

    @Test
    void selfOnlyRequiresSelfFilter() {
        DataScopeType type = DataScopeType.SELF_ONLY;
        assertThat(type.isAllData()).isFalse();
        assertThat(type.requiresDeptFilter()).isFalse();
        assertThat(type.requiresSelfFilter()).isTrue();
    }

    @Test
    void everyTypeHasDescription() {
        for (DataScopeType type : DataScopeType.values()) {
            assertThat(type.getDescription()).as("%s 描述非空", type).isNotBlank();
        }
        assertThat(DataScopeType.values()).hasSize(5);
    }
}
