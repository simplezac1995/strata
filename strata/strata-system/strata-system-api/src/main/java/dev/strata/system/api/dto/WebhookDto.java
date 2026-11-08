// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

public record WebhookDto(String id, String name, String url, String secret, boolean enabled) {}
