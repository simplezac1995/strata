// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.util.ArrayList;
import java.util.List;

public record MenuDto(
        String id,
        String parentId,
        String name,
        String type,
        String path,
        String component,
        String permission,
        int sortOrder,
        boolean visible,
        boolean enabled,
        List<MenuDto> children) {

    public MenuDto {
        if (children == null) {
            children = new ArrayList<>();
        }
    }
}
