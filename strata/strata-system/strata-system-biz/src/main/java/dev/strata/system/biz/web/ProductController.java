// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.ProductDto;
import dev.strata.system.api.dto.ProductQuery;
import dev.strata.system.api.request.ProductCreateRequest;
import dev.strata.system.api.request.ProductUpdateRequest;
import dev.strata.system.biz.demo.ProductAppService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/demo/products")
public class ProductController {

    private final ProductAppService productAppService;

    public ProductController(ProductAppService productAppService) {
        this.productAppService = productAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.DEMO_PRODUCT_LIST)
    public Result<PageResult<ProductDto>> page(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(productAppService.page(new ProductQuery(name, sku, enabled, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.DEMO_PRODUCT_LIST)
    public Result<ProductDto> get(@PathVariable Long id) {
        return Result.ok(productAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.DEMO_PRODUCT_CREATE)
    public Result<ProductDto> create(@Valid @RequestBody ProductCreateRequest request) {
        return Result.ok(productAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.DEMO_PRODUCT_UPDATE)
    public Result<ProductDto> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return Result.ok(productAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.DEMO_PRODUCT_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        productAppService.delete(id);
        return Result.ok();
    }
}
