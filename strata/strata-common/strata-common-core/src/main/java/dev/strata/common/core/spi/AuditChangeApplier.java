// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.spi;

/**
 * Optional write-back for audit change records: resolves the audited entity and re-applies the recorded change.
 *
 * <p>The framework records audit changes but cannot know how a host wants to replay them (soft delete, restore,
 * approval workflow). The default bean refuses the apply; a deployment supplies its own implementation.
 */
public interface AuditChangeApplier {

    /**
     * Applies a recorded change.
     *
     * @return {@code true} when the change was applied, {@code false} when this applier does not handle it
     */
    boolean apply(String entityName, Long entityId, String changesJson, String operator);
}
