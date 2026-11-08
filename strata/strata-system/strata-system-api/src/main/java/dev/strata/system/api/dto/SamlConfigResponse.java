// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

/** Public SAML login configuration for the login page. */
public record SamlConfigResponse(boolean enabled, String displayName) {}
