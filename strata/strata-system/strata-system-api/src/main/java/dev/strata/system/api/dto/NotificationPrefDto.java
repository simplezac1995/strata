// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

public record NotificationPrefDto(
        boolean emailEnabled, boolean inAppEnabled, boolean marketingEnabled, boolean securityEnabled) {}
