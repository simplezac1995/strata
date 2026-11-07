// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Upload the plain-text body of an already-created {@code sys_ai_document}. The document row only stores
 * metadata, so this payload feeds the chunking + keyword retrieval pipeline.
 */
public record AiDocumentContentRequest(@NotBlank @Size(max = 1_000_000) String text) {}
