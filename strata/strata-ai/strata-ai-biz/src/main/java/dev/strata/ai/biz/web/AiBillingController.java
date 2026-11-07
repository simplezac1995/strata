// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.web;

import dev.strata.ai.api.dto.AiBillingSummary;
import dev.strata.ai.api.dto.AiUsageAggregateItem;
import dev.strata.ai.biz.service.AiBillingAppService;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class AiBillingController {

    private final AiBillingAppService aiBillingAppService;

    public AiBillingController(AiBillingAppService aiBillingAppService) {
        this.aiBillingAppService = aiBillingAppService;
    }

    @GetMapping("/api/v1/ai/billing/summary")
    @PreAuthorize(Permissions.AI_CHAT_LIST)
    public Result<AiBillingSummary> summary(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(aiBillingAppService.summary(days));
    }

    @GetMapping("/api/v1/ai/usage/aggregate")
    @PreAuthorize(Permissions.AI_CHAT_LIST)
    public Result<List<AiUsageAggregateItem>> aggregate(
            @RequestParam(defaultValue = "30") int days, @RequestParam(defaultValue = "model") String groupBy) {
        return Result.ok(aiBillingAppService.aggregate(days, groupBy));
    }
}
