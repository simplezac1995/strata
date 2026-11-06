// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnouncementCreateRequest(
        @NotBlank @Size(max = 128) String title, @Size(max = 2000) String content, Boolean enabled) {}
