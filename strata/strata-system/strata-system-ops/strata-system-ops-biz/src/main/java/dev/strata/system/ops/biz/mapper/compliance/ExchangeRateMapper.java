// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper.compliance;

import dev.strata.system.api.dto.compliance.ExchangeRateDto;
import dev.strata.system.api.request.compliance.ExchangeRateCreateRequest;
import dev.strata.system.ops.api.entity.compliance.ExchangeRate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExchangeRateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ExchangeRate toEntity(ExchangeRateCreateRequest request);

    @Mapping(target = "id", expression = "java(String.valueOf(row.getId()))")
    ExchangeRateDto toDto(ExchangeRate row);
}
