// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.util.List;

public record RoleDto(String id, String code, String name, boolean enabled, List<String> menuIds) {}
