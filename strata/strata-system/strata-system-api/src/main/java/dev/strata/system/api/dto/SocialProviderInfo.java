// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

/** One configured SSO provider as exposed to the login page. */
public record SocialProviderInfo(String id, String displayName) {}
