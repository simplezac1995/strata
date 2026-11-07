// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.dto;

/**
 * Response of an AI office text-transform action. {@code usage} is the provider-reported token breakdown for
 * the call; it is {@code null} when the transform did not hit an LLM (e.g. unsupported action rejected
 * earlier).
 */
public record AiOfficeResponse(String result, AiTokenUsageDto usage) {

    /** Backward-compatible constructor for content-only responses (no metered LLM call). */
    public AiOfficeResponse(String result) {
        this(result, null);
    }
}
