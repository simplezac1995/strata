// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

public record ProductQuery(String name, String sku, Boolean enabled, Integer page, Integer size) {}
