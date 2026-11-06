// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;

/** Authorization-code callback from the OIDC provider; state and PKCE verifier are verified server-side. */
public record SocialCallbackRequest(@NotBlank String provider, @NotBlank String code, @NotBlank String state) {}
