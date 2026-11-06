// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.event;

public record OperationLogEvent(String module, String action, Long operatorId, String operatorName, String detail) {}
