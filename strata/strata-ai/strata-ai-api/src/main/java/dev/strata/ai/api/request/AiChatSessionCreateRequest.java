// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatSessionCreateRequest(@NotBlank @Size(max = 128) String title, String modelId) {}
