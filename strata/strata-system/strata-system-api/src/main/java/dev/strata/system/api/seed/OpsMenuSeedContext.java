// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.seed;

import java.util.List;

/** Context for optional domain menu seeds (AI / Pro) without biz↔biz dependencies. */
public interface OpsMenuSeedContext {

    boolean existsByPermission(String permission);

    /** Directory id when path+type already exists. */
    Long findDirectoryId(String path, String type);

    /** True when a child menu row with this path already exists under {@code parentId} (idempotency marker). */
    boolean existsChild(long parentId, String path);

    /** Creates a directory menu when missing; returns its id. */
    long ensureDirectory(String name, String path, int sortOrder, Long parentId);

    /**
     * Creates a menu row plus standard CRUD buttons (or update-only for settings-style resources).
     * Returns all created menu ids (parent + buttons).
     */
    List<Long> addMenuWithButtons(
            String name, String path, String component, String permissionPrefix, long parentId, int sortOrder);

    /** Adds a single menu row without buttons. */
    long addMenu(String name, String path, String component, String permission, long parentId, int sortOrder);

    /** Adds a button permission under parent menu. */
    long addButton(String name, String permission, long parentId, int sortOrder);

    void linkAdminRole(List<Long> menuIds);
}
