// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Admin broadcast body: one notification is fanned out to every enabled staff user. {@code category} is one
 * of {@code system}, {@code security}, {@code business}.
 */
public record NotificationBroadcastRequest(
        @NotBlank
                @Pattern(regexp = "system|security|business", message = "category must be system, security or business")
                String category,
        @NotBlank @Size(max = 128) String title,
        @Size(max = 2000) String content,
        @Size(max = 512) String actionUrl) {}
