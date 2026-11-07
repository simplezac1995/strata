// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.SearchResultsDto;
import dev.strata.system.biz.search.SearchAppService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/system")
public class SearchController {

    private final SearchAppService searchAppService;

    public SearchController(SearchAppService searchAppService) {
        this.searchAppService = searchAppService;
    }

    @GetMapping("/search")
    @PreAuthorize(Permissions.SYSTEM_SEARCH_LIST)
    public Result<SearchResultsDto> search(@RequestParam String q, @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(searchAppService.search(q, limit));
    }
}
