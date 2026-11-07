// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;

/** One-time code produced by the SAML ACS, exchanged by the SPA for framework tokens. */
public record SamlExchangeRequest(@NotBlank String code) {}
