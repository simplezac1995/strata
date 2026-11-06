// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.demo;

import dev.strata.system.api.dto.ProductDto;
import dev.strata.system.api.entity.DemoProduct;
import dev.strata.system.api.request.ProductCreateRequest;
import dev.strata.system.api.request.ProductUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    DemoProduct toEntity(ProductCreateRequest request);

    ProductDto toDto(DemoProduct product);

    @Mapping(target = "id", ignore = true)
    void update(ProductUpdateRequest request, @MappingTarget DemoProduct product);
}
