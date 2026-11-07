// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.seed;

import java.util.List;

record MenuSeedFile(List<MenuNodeSeed> menus) {

    record MenuNodeSeed(
            String key,
            String name,
            String type,
            String path,
            String component,
            String permission,
            String permissionBase,
            Boolean crudButtons,
            int sortOrder,
            List<MenuNodeSeed> children,
            List<ButtonSeed> buttons) {}

    record ButtonSeed(String name, String permission, int sortOrder) {}
}
