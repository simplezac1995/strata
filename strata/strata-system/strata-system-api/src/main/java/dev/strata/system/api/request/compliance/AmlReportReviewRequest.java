// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AmlReportReviewRequest(
        @NotBlank @Pattern(regexp = "pending|reviewed|cleared|flagged") String status, @Size(max = 1024) String note) {}
