// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

public record EmailTemplateDto(String id, String code, String name, String subject, String body, boolean enabled) {}
