// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.spi;

import java.util.Optional;

/**
 * Executes a database backup requested through the operations module.
 *
 * <p>Backups are environment-specific (pg_dump, managed snapshot, object storage), so the framework only records
 * the request; the default bean does not execute anything. A deployment supplies an executor that performs the
 * backup and returns a human-readable location/description.
 */
public interface BackupExecutor {

    /**
     * Executes a backup.
     *
     * @param requestedPath optional destination suggested by the operator
     * @return a human-readable location/result when a backup was executed, empty when backups are not configured
     */
    Optional<String> execute(String requestedPath);
}
