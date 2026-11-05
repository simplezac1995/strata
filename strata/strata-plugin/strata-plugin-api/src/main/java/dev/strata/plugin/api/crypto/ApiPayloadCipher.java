// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.api.crypto;

/** Optional request/response payload encryption (Pro). */
public interface ApiPayloadCipher {

    byte[] encrypt(byte[] plain);

    byte[] decrypt(byte[] encrypted);
}
