// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.demo;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.data.spec.Specs;
import dev.strata.system.api.dto.ProductDto;
import dev.strata.system.api.dto.ProductQuery;
import dev.strata.system.api.entity.DemoProduct;
import dev.strata.system.api.request.ProductCreateRequest;
import dev.strata.system.api.request.ProductUpdateRequest;
import dev.strata.system.biz.repository.DemoProductRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductAppService {

    private static final Logger log = LoggerFactory.getLogger(ProductAppService.class);

    private final DemoProductRepository demoProductRepository;
    private final ProductMapper productMapper;

    public ProductAppService(DemoProductRepository demoProductRepository, ProductMapper productMapper) {
        this.demoProductRepository = demoProductRepository;
        this.productMapper = productMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<ProductDto> page(ProductQuery query) {
        var spec = Specs.of(DemoProduct.class)
                .like("name", query.name())
                .eq("sku", query.sku())
                .eq("enabled", query.enabled())
                .toPredicate();
        var pageable = PageRequests.of(query.page(), query.size(), Sort.by(Sort.Direction.DESC, "id"));
        Page<DemoProduct> page = demoProductRepository.findAll(spec, pageable);
        return PageResult.from(page.map(productMapper::toDto));
    }

    @Transactional(readOnly = true)
    public ProductDto get(Long id) {
        return productMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "demo:product", action = "create")
    public ProductDto create(ProductCreateRequest request) {
        if (demoProductRepository.existsBySku(request.sku())) {
            throw new BusinessException(ErrorCode.CONFLICT, "SKU already exists");
        }
        DemoProduct product = productMapper.toEntity(request);
        demoProductRepository.save(product);
        return productMapper.toDto(product);
    }

    @Transactional
    @OperationLog(module = "demo:product", action = "update")
    public ProductDto update(Long id, ProductUpdateRequest request) {
        DemoProduct product = require(id);
        if (demoProductRepository.existsBySkuAndIdNot(request.sku(), id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "SKU already exists");
        }
        productMapper.update(request, product);
        demoProductRepository.save(product);
        return productMapper.toDto(product);
    }

    @Transactional
    @OperationLog(module = "demo:product", action = "delete")
    public void delete(Long id) {
        demoProductRepository.delete(require(id));
    }

    @Transactional(readOnly = true)
    public List<ProductDto> list() {
        return demoProductRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(productMapper::toDto)
                .toList();
    }

    private DemoProduct require(Long id) {
        return demoProductRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
    }
}
