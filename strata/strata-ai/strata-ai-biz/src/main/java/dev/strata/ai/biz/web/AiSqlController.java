// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.web;

import dev.strata.ai.api.dto.Chat2SqlResponse;
import dev.strata.ai.api.request.Chat2SqlRequest;
import dev.strata.ai.biz.service.AiSqlAppService;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/ai/sql")
public class AiSqlController {

    private final AiSqlAppService aiSqlAppService;

    public AiSqlController(AiSqlAppService aiSqlAppService) {
        this.aiSqlAppService = aiSqlAppService;
    }

    @PostMapping("/chat2sql")
    @PreAuthorize(Permissions.AI_CHAT_LIST)
    public Result<Chat2SqlResponse> chat2Sql(@Valid @RequestBody Chat2SqlRequest request) {
        return Result.ok(aiSqlAppService.chat2Sql(request));
    }

    @GetMapping("/tables")
    @PreAuthorize(Permissions.AI_CHAT_LIST)
    public Result<List<Map<String, Object>>> listTables() {
        return Result.ok(aiSqlAppService.listTables());
    }

    @GetMapping("/columns")
    @PreAuthorize(Permissions.AI_CHAT_LIST)
    public Result<List<Map<String, Object>>> listColumns(@RequestParam String table) {
        return Result.ok(aiSqlAppService.listColumns(table));
    }
}
