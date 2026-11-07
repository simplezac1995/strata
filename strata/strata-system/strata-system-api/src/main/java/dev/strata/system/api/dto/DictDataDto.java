// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

public record DictDataDto(String id, String typeId, String value, String label, int sortOrder, boolean enabled) {}
