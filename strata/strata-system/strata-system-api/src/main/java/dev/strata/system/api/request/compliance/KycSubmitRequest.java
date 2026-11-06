// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record KycSubmitRequest(
        @NotBlank @Size(max = 128) String fullName,
        @NotBlank @Pattern(regexp = "id_card|passport") String idType,
        @NotBlank @Size(max = 128) String idNumberHash,
        @Size(max = 512) String frontImageUrl,
        @Size(max = 512) String backImageUrl,
        @Size(max = 512) String selfieUrl) {}
