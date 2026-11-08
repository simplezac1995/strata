// SPDX-License-Identifier: Apache-2.0
package dev.strata.example.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The module's public contract: the only shape another module or the host application should depend on.
 *
 * <p>A real feature module keeps request/response DTOs in its {@code api} package and returns them from its web layer;
 * the host application (and other modules) then never needs the {@code service} or persistence classes.
 *
 * @param id server-assigned identifier
 * @param title short human-readable title
 * @param body free-form note body
 */
public record ExampleNote(String id, @NotBlank @Size(max = 80) String title, @Size(max = 2000) String body) {}
