// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.api.dto;

public record AiVectorStoreDto(String id, String name, String type, String host, int dimensions, boolean enabled) {}
