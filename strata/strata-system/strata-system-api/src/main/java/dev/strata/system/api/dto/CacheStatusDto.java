// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

public record CacheStatusDto(
        String source,
        boolean stub,
        String redisVersion,
        String usedMemory,
        Long totalKeys,
        Long connectedClients,
        Double hitRate) {

    public CacheStatusDto(String source, boolean stub) {
        this(source, stub, null, null, null, null, null);
    }
}
