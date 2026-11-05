// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

public record PaymentCreatedDto(String id, String orderNo, String checkoutUrl) {}
