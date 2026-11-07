// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.security;

import java.util.List;

/**
 * Module-owned declaration of unauthenticated request paths (ADR-0021).
 *
 * <p>Replaces appending module endpoints (webhooks, callbacks) to the central whitelist inside
 * {@code SecurityAutoConfiguration}: the security assembly injects every registered
 * {@link PermitPathContributor} and merges their paths into {@code requestMatchers(...).permitAll()}.
 * A contributor bean exists only while its module is enabled (mirror {@code @ConditionalOnProperty}),
 * so disabling the module also removes its public paths from the security surface.
 *
 * <p>Only answers "does this path require login?" — method-level rules stay on {@code @PreAuthorize}.
 * Lives in common-core (not common-security) so plugins and domain modules can implement it without
 * depending on the security module (plugin dependency rule: plugin-api + common-core).
 */
@FunctionalInterface
public interface PermitPathContributor {

    /** Ant-style request paths that must bypass JWT authentication. */
    List<String> permitPaths();
}
