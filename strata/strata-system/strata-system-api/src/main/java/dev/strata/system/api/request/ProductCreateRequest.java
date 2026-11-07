// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Size(max = 64) String sku,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @Min(0) int stock,
        @Size(max = 512) String description,
        boolean enabled) {}
