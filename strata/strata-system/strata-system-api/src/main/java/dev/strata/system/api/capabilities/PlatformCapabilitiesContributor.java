// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.capabilities;

import dev.strata.system.api.dto.PlatformCapabilitiesDto;

/** Optional extension point for edition-specific capability flags (Enterprise, plugins). */
public interface PlatformCapabilitiesContributor {

    PlatformCapabilitiesDto apply(PlatformCapabilitiesDto base);
}
