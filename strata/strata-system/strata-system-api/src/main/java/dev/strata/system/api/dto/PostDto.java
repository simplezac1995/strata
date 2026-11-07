// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

public record PostDto(String id, String code, String name, int sortOrder, boolean enabled) {}
