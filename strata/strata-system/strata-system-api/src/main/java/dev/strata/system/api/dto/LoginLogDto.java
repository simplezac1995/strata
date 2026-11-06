// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

public record LoginLogDto(String id, String username, String ip, String browser, String status, Instant createdAt) {}
