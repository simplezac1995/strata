// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

public record TenantDto(String id, String code, String name, String domain, String status, String plan) {}
