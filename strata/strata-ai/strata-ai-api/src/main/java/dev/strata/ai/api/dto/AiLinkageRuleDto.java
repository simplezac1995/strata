// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.dto;

/** An AI linkage rule row as returned to the admin console. */
public record AiLinkageRuleDto(
        String id, String name, String object, String event, String action, boolean enabled, String description) {}
