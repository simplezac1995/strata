// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper.compliance;

import dev.strata.system.api.dto.compliance.AmlRuleDto;
import dev.strata.system.api.request.compliance.AmlRuleCreateRequest;
import dev.strata.system.api.request.compliance.AmlRuleUpdateRequest;
import dev.strata.system.ops.api.entity.compliance.AmlRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AmlRuleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    AmlRule toEntity(AmlRuleCreateRequest request);

    @Mapping(target = "id", expression = "java(String.valueOf(row.getId()))")
    AmlRuleDto toDto(AmlRule row);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void update(AmlRuleUpdateRequest request, @MappingTarget AmlRule row);
}
