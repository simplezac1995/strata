// SPDX-License-Identifier: Apache-2.0
import { ApiError, apiGet, apiPost } from "@/shared/api/client";
import type { TokenResponse } from "@/shared/types";

/** Public provider information returned by the login-page config endpoint. */
export type SocialProviderInfo = { id: string; displayName: string };

/** Public social/OIDC configuration. */
export type SocialConfig = {
  enabled: boolean;
  providers: SocialProviderInfo[];
  allowedRedirectUris: string[];
};

const PROVIDER_KEY = "strata_sso_provider";
const STATE_KEY = "strata_sso_state";

/** The callback route is fixed; it must be in the server's `strata.social.allowed-redirect-uris` whitelist. */
export function ssoRedirectUri(): string {
  return `${window.location.origin}/sso-callback`;
}

/** Fetches the login-page config; failures degrade gracefully to "SSO unavailable". */
export async function fetchSocialConfig(): Promise<SocialConfig> {
  try {
    const config = await apiGet<SocialConfig>("/api/v1/system/auth/social/config");
    return {
      enabled: Boolean(config?.enabled),
      providers: config?.providers ?? [],
      allowedRedirectUris: config?.allowedRedirectUris ?? [],
    };
  } catch {
    return { enabled: false, providers: [], allowedRedirectUris: [] };
  }
}

/** Starts the authorization-code + PKCE flow and redirects the browser to the provider. */
export async function startSso(provider: string): Promise<void> {
  const redirectUri = ssoRedirectUri();
  const response = await apiGet<{ state: string; authorizeUrl: string }>(
    `/api/v1/system/auth/social/authorize?provider=${encodeURIComponent(provider)}&redirectUri=${encodeURIComponent(redirectUri)}`,
  );
  sessionStorage.setItem(PROVIDER_KEY, provider);
  sessionStorage.setItem(STATE_KEY, response.state);
  window.location.assign(response.authorizeUrl);
}

/** Removes and returns the provider stored when the flow started. */
export function takeSsoProvider(): string | null {
  const provider = sessionStorage.getItem(PROVIDER_KEY);
  sessionStorage.removeItem(PROVIDER_KEY);
  return provider;
}

/** Removes and returns the state stored when the flow started (client-side CSRF check). */
export function takeSsoState(): string | null {
  const state = sessionStorage.getItem(STATE_KEY);
  sessionStorage.removeItem(STATE_KEY);
  return state;
}

/** Exchanges the authorization code server-side and returns a framework session. */
export function completeSso(provider: string, code: string, state: string): Promise<TokenResponse> {
  return apiPost<TokenResponse>("/api/v1/system/auth/social/callback", { provider, code, state });
}

/** Best-effort message for the callback page. */
export function ssoErrorMessage(error: unknown): string | null {
  if (error instanceof ApiError) return error.message;
  return null;
}
