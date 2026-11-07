// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.seed;

/** Domain-specific ops menu seeds (AI / Pro menus). */
public interface OpsMenuSeedContributor {

    void seed(OpsMenuSeedContext context);
}
