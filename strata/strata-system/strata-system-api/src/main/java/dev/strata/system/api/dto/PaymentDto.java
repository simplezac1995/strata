// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

/** Demo payment list/detail DTO (entity {@code DemoPayment}, API layer drops Demo prefix). */
public record PaymentDto(String id, String orderNo, long amountCents, String currency, String status) {}
