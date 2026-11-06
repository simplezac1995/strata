// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.dto;

public record AiLabelSampleDto(String id, String taskId, String content, String label, String source) {}
