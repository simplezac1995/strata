// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper;

import dev.strata.system.api.dto.RiskRuleDto;
import dev.strata.system.api.request.RiskRuleCreateRequest;
import dev.strata.system.api.request.RiskRuleUpdateRequest;
import dev.strata.system.ops.api.entity.RiskRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RiskRuleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    RiskRule toEntity(RiskRuleCreateRequest request);

    @Mapping(target = "id", expression = "java(String.valueOf(rule.getId()))")
    RiskRuleDto toDto(RiskRule rule);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void update(RiskRuleUpdateRequest request, @MappingTarget RiskRule rule);
}
