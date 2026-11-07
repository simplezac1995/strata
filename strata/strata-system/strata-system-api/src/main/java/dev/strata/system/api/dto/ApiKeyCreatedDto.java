// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

/** Returned only from create — includes the plaintext key once. */
public record ApiKeyCreatedDto(
        String id,
        String name,
        String keyPrefix,
        String plaintextKey,
        String scopes,
        boolean enabled,
        Instant createdAt) {}
