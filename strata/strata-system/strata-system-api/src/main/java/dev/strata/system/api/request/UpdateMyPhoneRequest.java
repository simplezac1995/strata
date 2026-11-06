// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Self-service binding of the current user's mobile number (no SMS OTP on this batch). */
public record UpdateMyPhoneRequest(@NotBlank @Size(max = 32) String phone) {}
