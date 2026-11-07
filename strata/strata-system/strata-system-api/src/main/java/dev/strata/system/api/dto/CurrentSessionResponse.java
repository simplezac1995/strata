// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.util.List;

/** Current session snapshot used by the admin console to refresh its user + menu tree without re-login. */
public record CurrentSessionResponse(UserDto user, List<MenuDto> menus) {}
