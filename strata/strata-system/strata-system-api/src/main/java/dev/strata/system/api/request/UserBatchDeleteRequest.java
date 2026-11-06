// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UserBatchDeleteRequest(@NotEmpty List<Long> ids) {}
