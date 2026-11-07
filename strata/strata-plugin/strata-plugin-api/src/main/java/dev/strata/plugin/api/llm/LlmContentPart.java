// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.api.llm;

/**
 * One multimodal content part of an OpenAI-compatible chat message. {@code type} is {@code "text"}
 * or {@code "image_url"}; only the matching field ({@code text} / {@code imageUrl}) is populated for a
 * given part.
 */
public record LlmContentPart(String type, String text, String imageUrl) {}
