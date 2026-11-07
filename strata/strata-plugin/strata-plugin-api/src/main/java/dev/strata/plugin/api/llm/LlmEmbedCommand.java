// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.api.llm;

/**
 * Embedding request: text to embed + which model/provider to use.
 */
public record LlmEmbedCommand(String baseUrl, String apiKey, String modelCode, String text) {}
