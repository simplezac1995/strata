// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.scim;

import dev.strata.common.core.security.PermitPathContributor;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** SCIM endpoints are machine-to-machine; {@link ScimAuthFilter} authenticates them with a bearer token. */
@Component
@ConditionalOnProperty(prefix = "strata.scim", name = "enabled", havingValue = "true")
public class ScimPermitPathContributor implements PermitPathContributor {

    @Override
    public List<String> permitPaths() {
        return List.of("/scim/v2/**");
    }
}
