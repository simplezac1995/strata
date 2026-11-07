// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.api.status;

/**
 * Plugin status self-declaration SPI (ADR-0020: modules declare their own status).
 *
 * <p>Each plugin module owns exactly one {@link PluginStatusProvider} so the core does not have to
 * know every module: {@code PluginStatusController} injects the full {@code List<PluginStatusProvider>}
 * and iterates it to build {@code GET /api/v1/plugins/status}. Adding a plugin no longer requires
 * touching the controller.
 *
 * <p>Registration is conditional on the module being enabled: a plugin whose switch is off (or whose
 * starter is not assembled at all) registers no provider bean and therefore contributes no status
 * key. When the provider is registered, {@link #available()} reports the runtime usability, which is
 * re-evaluated on every request (computed, never persisted).
 *
 * <p>{@link #key()} must be the camelCase short key the admin UI already expects (e.g.
 * {@code storageLocal}, {@code paymentStripe}, {@code llmOpenAi}, {@code excel}, {@code encryptAes})
 * so the JSON contract of {@code /plugins/status} is preserved.
 */
public interface PluginStatusProvider {

    /** camelCase short key surfaced under {@code data} of {@code GET /api/v1/plugins/status}. */
    String key();

    /** Human-readable plugin label (informational; may be surfaced by the ops UI later). */
    String label();

    /** Whether the plugin is currently usable at runtime (SPI bean present / configured). */
    boolean available();

    /** Optional human-readable detail describing the current runtime state. */
    String detail();
}
