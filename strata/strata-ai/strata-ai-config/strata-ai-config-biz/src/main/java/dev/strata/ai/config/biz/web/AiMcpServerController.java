// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.web;

import dev.strata.ai.api.request.AiMcpServerCreateRequest;
import dev.strata.ai.api.request.AiMcpServerUpdateRequest;
import dev.strata.ai.config.api.dto.AiMcpServerDto;
import dev.strata.ai.config.biz.service.AiMcpServerAppService;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/ai/mcp-servers")
public class AiMcpServerController {

    private final AiMcpServerAppService aiMcpServerAppService;

    public AiMcpServerController(AiMcpServerAppService aiMcpServerAppService) {
        this.aiMcpServerAppService = aiMcpServerAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.AI_MCP_LIST)
    public Result<List<AiMcpServerDto>> list() {
        return Result.ok(aiMcpServerAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.AI_MCP_LIST)
    public Result<PageResult<AiMcpServerDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(aiMcpServerAppService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.AI_MCP_LIST)
    public Result<AiMcpServerDto> get(@PathVariable Long id) {
        return Result.ok(aiMcpServerAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.AI_MCP_CREATE)
    public Result<AiMcpServerDto> create(@Valid @RequestBody AiMcpServerCreateRequest request) {
        return Result.ok(aiMcpServerAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.AI_MCP_UPDATE)
    public Result<AiMcpServerDto> update(@PathVariable Long id, @Valid @RequestBody AiMcpServerUpdateRequest request) {
        return Result.ok(aiMcpServerAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.AI_MCP_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        aiMcpServerAppService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/test")
    @PreAuthorize(Permissions.AI_MCP_UPDATE)
    public Result<java.util.Map<String, Object>> test(@PathVariable Long id) {
        return Result.ok(aiMcpServerAppService.testConnection(id));
    }

    @GetMapping("/{id}/tools")
    @PreAuthorize(Permissions.AI_MCP_LIST)
    public Result<List<java.util.Map<String, Object>>> tools(@PathVariable Long id) {
        return Result.ok(aiMcpServerAppService.listTools(id));
    }
}
