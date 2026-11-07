// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

public record RegionDto(String id, String code, String name, int level, String parentId) {}
