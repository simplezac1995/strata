// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Manual backup trigger. The real pg_dump execution is a TODO; this just records intent.
 */
public record BackupCreateRequest(@NotBlank @Size(max = 16) String type, @Size(max = 512) String path) {}
