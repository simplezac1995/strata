// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.dto;

import dev.strata.common.data.desensitize.Desensitize;
import dev.strata.common.data.desensitize.DesensitizeType;

public record AiProviderDto(
        String id,
        String code,
        String name,
        String type,
        String baseUrl,
        @Desensitize(type = DesensitizeType.API_KEY) String apiKey,
        boolean enabled) {}
