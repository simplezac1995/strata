// SPDX-License-Identifier: Apache-2.0
package {{PKG}}.biz.{{MODULE}};

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import {{PKG}}.api.dto.{{E}}Dto;
import {{PKG}}.api.dto.{{E}}Query;
import {{PKG}}.api.request.{{E}}CreateRequest;
import {{PKG}}.api.request.{{E}}UpdateRequest;
import jakarta.validation.Valid;
{{CONTROLLER_IMPORTS}}import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/{{MODULE}}/{{RESOURCE}}")
public class {{E}}Controller {

    private final {{E}}AppService {{EL}}AppService;

    public {{E}}Controller({{E}}AppService {{EL}}AppService) {
        this.{{EL}}AppService = {{EL}}AppService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('{{PERM}}:list')")
    public Result<PageResult<{{E}}Dto>> page(
{{CONTROLLER_PARAMS}}            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(
                {{EL}}AppService.page(new {{E}}Query({{QUERY_CONSTRUCTOR_ARGS}})));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('{{PERM}}:list')")
    public Result<{{E}}Dto> get(@PathVariable Long id) {
        return Result.ok({{EL}}AppService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('{{PERM}}:create')")
    public Result<{{E}}Dto> create(@Valid @RequestBody {{E}}CreateRequest request) {
        return Result.ok({{EL}}AppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('{{PERM}}:update')")
    public Result<{{E}}Dto> update(@PathVariable Long id, @Valid @RequestBody {{E}}UpdateRequest request) {
        return Result.ok({{EL}}AppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('{{PERM}}:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        {{EL}}AppService.delete(id);
        return Result.ok();
    }
}
