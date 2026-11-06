// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.config;

import dev.strata.common.core.security.PermitPathContributor;
import java.util.List;
import org.springframework.stereotype.Component;

/** Public paths of the login handshake: captcha/login are core, the social endpoints live in this module. */
@Component
public class RbacPermitPathContributor implements PermitPathContributor {

    @Override
    public List<String> permitPaths() {
        return List.of(
                "/api/v1/system/auth/social/config",
                "/api/v1/system/auth/social/authorize",
                "/api/v1/system/auth/social/callback");
    }
}
