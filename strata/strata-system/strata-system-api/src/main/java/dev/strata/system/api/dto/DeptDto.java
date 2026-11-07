// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.util.ArrayList;
import java.util.List;

public record DeptDto(String id, String parentId, String name, int sortOrder, boolean enabled, List<DeptDto> children) {

    public DeptDto {
        if (children == null) {
            children = new ArrayList<>();
        }
    }
}
