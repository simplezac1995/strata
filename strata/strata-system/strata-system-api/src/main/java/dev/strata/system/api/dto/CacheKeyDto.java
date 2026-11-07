// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

public record CacheKeyDto(String key, String type, Long ttlSeconds) {}
