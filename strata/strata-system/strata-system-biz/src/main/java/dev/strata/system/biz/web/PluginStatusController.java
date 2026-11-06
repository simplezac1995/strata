// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.plugin.api.status.PluginStatusProvider;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Runtime plugin availability, self-declared by each plugin module (ADR-0020).
 *
 * <p>The controller no longer hard-codes keys: it collects every registered {@link PluginStatusProvider}
 * into the {@code data} map of {@code GET /api/v1/plugins/status}. A provider exists only while its
 * module is enabled, so disabled plugins simply contribute no key (the admin UI treats a missing key
 * as {@code false}). Response keys stay the camelCase short keys the front end reads, e.g.
 * {@code storageLocal}, {@code paymentStripe}.
 */
@RestController
@RequestMapping("/api/v1/plugins")
public class PluginStatusController {

    private final List<PluginStatusProvider> providers;

    public PluginStatusController(List<PluginStatusProvider> providers) {
        this.providers = providers;
    }

    @GetMapping("/status")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public Result<Map<String, Boolean>> status() {
        Map<String, Boolean> status = new LinkedHashMap<>();
        for (PluginStatusProvider provider : providers) {
            status.put(provider.key(), provider.available());
        }
        return Result.ok(status);
    }
}
