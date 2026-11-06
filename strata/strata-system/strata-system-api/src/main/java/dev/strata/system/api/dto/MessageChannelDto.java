// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

public record MessageChannelDto(String id, String channelType, String configJson, boolean enabled) {}
