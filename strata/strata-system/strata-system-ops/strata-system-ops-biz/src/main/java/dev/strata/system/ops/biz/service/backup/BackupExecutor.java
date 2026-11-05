// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service.backup;

/**
 * Runs the on-disk backup script ({@code scripts/backup.sh} / {@code backup.ps1}) and reports what it produced.
 *
 * <p>Extracted behind an interface so the money/ops service can be unit-tested without actually invoking
 * {@code pg_dump}; the production implementation shells out through {@link ProcessBuilder}.
 */
public interface BackupExecutor {

    /**
     * Executes a backup of the given type.
     *
     * @param type          {@code full} or {@code incremental}
     * @param requestedPath caller-suggested output directory (may be blank; the script has its own default)
     * @return the produced artifact and whether the script succeeded
     */
    BackupOutcome run(String type, String requestedPath);

    /** Outcome of one backup run. */
    record BackupOutcome(boolean success, String filename, String path, long sizeBytes, String error) {

        public static BackupOutcome ok(String filename, String path, long sizeBytes) {
            return new BackupOutcome(true, filename, path, sizeBytes, null);
        }

        public static BackupOutcome failed(String error) {
            return new BackupOutcome(false, null, null, 0L, error);
        }
    }
}
