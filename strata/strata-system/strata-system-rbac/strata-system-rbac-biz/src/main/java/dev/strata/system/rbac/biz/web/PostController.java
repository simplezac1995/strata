// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.PostDto;
import dev.strata.system.api.request.PostCreateRequest;
import dev.strata.system.api.request.PostUpdateRequest;
import dev.strata.system.rbac.biz.service.PostAppService;
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
@RequestMapping("/api/v1/system/posts")
public class PostController {

    private final PostAppService postAppService;

    public PostController(PostAppService postAppService) {
        this.postAppService = postAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_POST_LIST)
    public Result<List<PostDto>> list() {
        return Result.ok(postAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.SYSTEM_POST_LIST)
    public Result<PageResult<PostDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(postAppService.page(page, size));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_POST_CREATE)
    public Result<PostDto> create(@Valid @RequestBody PostCreateRequest request) {
        return Result.ok(postAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_POST_UPDATE)
    public Result<PostDto> update(@PathVariable Long id, @Valid @RequestBody PostUpdateRequest request) {
        return Result.ok(postAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_POST_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        postAppService.delete(id);
        return Result.ok();
    }
}
