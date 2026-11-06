// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.dto;

public record AiLabelTaskDto(String id, String name, String type, String status, int sampleCount) {}
