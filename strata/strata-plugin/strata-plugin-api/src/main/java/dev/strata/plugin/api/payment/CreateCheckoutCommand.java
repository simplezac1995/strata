// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.api.payment;

public record CreateCheckoutCommand(
        String idempotencyKey, long amountCents, String currency, String successUrl, String cancelUrl) {}
