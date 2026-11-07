// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.web;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public PageResult {
        if (content == null) {
            content = List.of();
        }
    }

    public static <T> PageResult<T> from(Page<T> page) {
        return new PageResult<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), page, size, 0, 0);
    }
}
