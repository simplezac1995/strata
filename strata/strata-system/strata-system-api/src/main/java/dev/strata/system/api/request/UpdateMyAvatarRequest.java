// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Self-service avatar update: the URL was produced by the existing file upload endpoint. */
public record UpdateMyAvatarRequest(@NotBlank @Size(max = 512) String avatar) {}
