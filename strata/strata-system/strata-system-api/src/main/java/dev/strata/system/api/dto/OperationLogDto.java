// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

public record OperationLogDto(
        String id, String moduleName, String actionName, String operatorName, String detail, Instant createdAt) {}
