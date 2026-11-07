// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.util.List;

/** Public social/OIDC configuration for the login page. */
public record SocialConfigResponse(
        boolean enabled, List<SocialProviderInfo> providers, List<String> allowedRedirectUris) {}
