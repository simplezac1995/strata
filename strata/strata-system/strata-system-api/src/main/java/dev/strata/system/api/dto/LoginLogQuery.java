// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

public record LoginLogQuery(
        String username, String status, String ip, Instant from, Instant to, Integer page, Integer size) {}
