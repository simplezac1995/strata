// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Staff reply body: the message text plus an optional opaque JSON array of attachment descriptors. */
public record CsReplyRequest(@NotBlank @Size(max = 2000) String content, @Size(max = 1024) String attachments) {}
