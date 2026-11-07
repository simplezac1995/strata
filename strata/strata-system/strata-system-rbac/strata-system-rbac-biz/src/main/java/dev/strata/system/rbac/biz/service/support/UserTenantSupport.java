// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import dev.strata.common.data.tenant.TenantScopeSupport;
import dev.strata.common.security.tenant.TenantProperties;
import dev.strata.system.rbac.api.entity.User;
import org.springframework.data.jpa.domain.Specification;

public final class UserTenantSupport {

    private UserTenantSupport() {}

    public static Specification<User> tenantScope(TenantProperties tenantProperties) {
        return TenantScopeSupport.equalsTenantId(tenantProperties.isRowLevelEnabled(), "tenantId");
    }

    public static void applyTenantOnCreate(User user, TenantProperties tenantProperties) {
        TenantScopeSupport.applyTenantIdOnCreate(tenantProperties.isRowLevelEnabled(), user::setTenantId);
    }
}
