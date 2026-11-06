// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.math.BigDecimal;

public record ProductDto(
        String id, String name, String sku, BigDecimal price, int stock, String description, boolean enabled) {}
