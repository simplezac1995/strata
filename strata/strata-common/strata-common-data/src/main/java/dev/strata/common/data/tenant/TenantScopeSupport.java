// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.data.tenant;

import dev.strata.common.core.tenant.TenantContext;
import java.util.function.Consumer;
import org.springframework.data.jpa.domain.Specification;

public final class TenantScopeSupport {

    private TenantScopeSupport() {}

    public static <T> Specification<T> equalsTenantId(boolean rowLevelEnabled, String attribute) {
        if (!rowLevelEnabled) {
            return (root, query, cb) -> cb.conjunction();
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get(attribute), tenantId);
    }

    public static void applyTenantIdOnCreate(boolean rowLevelEnabled, Consumer<Long> setter) {
        if (!rowLevelEnabled) {
            return;
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            setter.accept(tenantId);
        }
    }
}
