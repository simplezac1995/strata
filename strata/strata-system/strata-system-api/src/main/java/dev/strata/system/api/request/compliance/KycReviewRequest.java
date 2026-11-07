// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request.compliance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KycReviewRequest(@NotNull boolean approve, @Size(max = 512) String rejectReason) {}
