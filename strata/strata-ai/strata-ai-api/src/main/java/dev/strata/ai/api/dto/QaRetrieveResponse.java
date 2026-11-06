// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.dto;

import java.util.List;

/**
 * Result of a grounded QA retrieval: the model's answer plus the chunks it was grounded on (citations).
 * When no chunk hits, {@code citations} is empty and {@code answer} is a direct (ungrounded) reply.
 */
public record QaRetrieveResponse(String answer, List<Citation> citations) {

    /** One retrieval hit backing the answer. */
    public record Citation(Long documentId, String documentName, Integer chunkIndex, String snippet) {}
}
