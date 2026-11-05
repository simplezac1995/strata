// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GeoCheckIpRequest(@NotBlank @Size(max = 64) String ip) {}
