// SPDX-License-Identifier: Apache-2.0
import { apiGet, apiPost } from "@/shared/api/client";
import type { TokenResponse } from "@/shared/types";

/** Login-page SAML configuration. */
export type SamlConfig = { enabled: boolean; displayName: string };

/** Fetches the SAML login-page config; failures degrade gracefully to "SAML unavailable". */
export async function fetchSamlConfig(): Promise<SamlConfig> {
  try {
    const config = await apiGet<SamlConfig>("/api/v1/system/auth/saml/config");
    return {
      enabled: Boolean(config?.enabled),
      displayName: config?.displayName || "SAML",
    };
  } catch {
    return { enabled: false, displayName: "SAML" };
  }
}

/** Starts the SP-initiated SAML flow through the backend (which redirects to the IdP). */
export function startSaml(): void {
  window.location.assign("/saml2/authenticate");
}

/** Exchanges the one-time code returned by the ACS for framework tokens. */
export function exchangeSaml(code: string): Promise<TokenResponse> {
  return apiPost<TokenResponse>("/api/v1/system/auth/saml/exchange", { code });
}
