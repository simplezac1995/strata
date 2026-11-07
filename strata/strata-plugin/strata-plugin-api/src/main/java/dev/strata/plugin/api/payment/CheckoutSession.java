// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.api.payment;

public record CheckoutSession(String sessionId, String checkoutUrl) {}
