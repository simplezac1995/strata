// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.Size;

public record AiLabelTaskUpdateRequest(
        @Size(max = 128) String name, @Size(max = 16) String type, @Size(max = 16) String status) {}
