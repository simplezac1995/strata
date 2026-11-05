// SPDX-License-Identifier: Apache-2.0
package dev.strata.example.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a note. Kept next to the response DTO because both are part of the module contract.
 *
 * @param title short human-readable title
 * @param body free-form note body
 */
public record ExampleNoteRequest(@NotBlank @Size(max = 80) String title, @Size(max = 2000) String body) {}
