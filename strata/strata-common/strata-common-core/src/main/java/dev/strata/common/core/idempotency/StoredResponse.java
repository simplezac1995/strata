// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

/**
 * The first response for a key, kept verbatim so a retry sees exactly what the original caller saw —
 * same status, same body. Shape-agnostic on purpose: the stored bytes are handed back untouched.
 */
public record StoredResponse(int status, String contentType, String body) {}
