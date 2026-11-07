// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.event;

/**
 * Published when an admin clicks "Test" on a linkage rule. The {@link
 * LinkageRuleEvaluator} treats this like a real business event so notify/tag/email
 * wiring can be verified without waiting for production traffic.
 */
public record LinkageTestEvent(String object, String event, String detail) {}
