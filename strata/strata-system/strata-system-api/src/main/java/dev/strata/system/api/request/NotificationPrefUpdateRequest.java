// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

public record NotificationPrefUpdateRequest(
        Boolean emailEnabled, Boolean inAppEnabled, Boolean marketingEnabled, Boolean securityEnabled) {}
