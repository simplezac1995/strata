// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.web;

import dev.strata.ai.api.dto.AiLinkageRuleDto;
import dev.strata.ai.api.request.AiLinkageRuleUpsertRequest;
import dev.strata.ai.biz.service.AiLinkageRuleAppService;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/ai/linkage/rules")
public class AiLinkageRuleController {

    private final AiLinkageRuleAppService appService;

    public AiLinkageRuleController(AiLinkageRuleAppService appService) {
        this.appService = appService;
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.AI_LINKAGE_LIST)
    public Result<PageResult<AiLinkageRuleDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(appService.page(page, size));
    }

    @GetMapping
    @PreAuthorize(Permissions.AI_LINKAGE_LIST)
    public Result<List<AiLinkageRuleDto>> list() {
        return Result.ok(appService.list());
    }

    @PostMapping
    @PreAuthorize(Permissions.AI_LINKAGE_CREATE)
    public Result<AiLinkageRuleDto> create(@Valid @RequestBody AiLinkageRuleUpsertRequest request) {
        return Result.ok(appService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.AI_LINKAGE_UPDATE)
    public Result<AiLinkageRuleDto> update(
            @PathVariable Long id, @Valid @RequestBody AiLinkageRuleUpsertRequest request) {
        return Result.ok(appService.update(id, request));
    }

    @PutMapping("/{id}/enabled")
    @PreAuthorize(Permissions.AI_LINKAGE_UPDATE)
    public Result<Void> setEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        appService.setEnabled(id, Boolean.TRUE.equals(body.get("enabled")));
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.AI_LINKAGE_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        appService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/test")
    @PreAuthorize(Permissions.AI_LINKAGE_UPDATE)
    public Result<Map<String, Object>> test(@PathVariable Long id) {
        appService.testRule(id);
        return Result.ok(Map.of("ok", true));
    }
}
