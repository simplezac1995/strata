// SPDX-License-Identifier: Apache-2.0
/**
 * Optional AES-GCM payload encryption flag when STRATA_ENCRYPT_AES_ENABLED=true on the API.
 *
 * Encryption is header-only: we signal to the server (and expect it to expect) encrypted
 * payloads via the `X-Strata-Encrypted` header. No request/response body is encrypted here.
 */
const ENCRYPT_HEADER = "X-Strata-Encrypted";

export function isApiEncryptionEnabled(): boolean {
  return import.meta.env.VITE_API_ENCRYPT === "true";
}

export function encryptionHeaders(enabled: boolean): Record<string, string> {
  return enabled ? { [ENCRYPT_HEADER]: "1" } : {};
}
