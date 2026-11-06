// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.Size;

public record MessageChannelUpdateRequest(@Size(max = 4000) String configJson, Boolean enabled) {}
