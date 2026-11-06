// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

public record OauthClientDto(
        String id, String clientId, String name, String clientSecret, String scopes, boolean enabled) {}
