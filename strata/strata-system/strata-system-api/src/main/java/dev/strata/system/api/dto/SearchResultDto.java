// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

/**
 * A single cross-entity search hit (#15). {@code id} is the target entity id as a String (Long ids are
 * rendered as strings per project convention); {@code route} is the front-end path to navigate to.
 */
public record SearchResultDto(String id, String label, String subtitle, String route) {}
