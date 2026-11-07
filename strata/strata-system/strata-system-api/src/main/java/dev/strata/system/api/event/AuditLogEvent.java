// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.event;

public record AuditLogEvent(
        String eventType,
        String module,
        String action,
        String resourceType,
        String resourceId,
        Long operatorId,
        String operatorName,
        String ipAddress,
        String userAgent,
        boolean success,
        String detail) {}
