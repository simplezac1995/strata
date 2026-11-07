// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import java.time.Instant;

/**
 * One attempt to claim an {@code Idempotency-Key}.
 *
 * @param scope who the key belongs to ({@code user:42}, {@code player:7}, …) — the same key sent by two
 *     different callers must not collide, so the unique index is on {@code (scope, key)}
 * @param key the raw header value
 * @param method HTTP method of the attempt
 * @param path request path *without* the context path, so a key cannot be replayed against another route
 * @param fingerprint SHA-256 over method + path + body; a key reused with a different payload is a bug,
 *     not a replay, and must be rejected instead of silently returning the first answer
 * @param expiresAt when the row may be purged; until then the key is reserved
 */
public record IdempotencyRequest(
        String scope, String key, String method, String path, String fingerprint, Instant expiresAt) {}
