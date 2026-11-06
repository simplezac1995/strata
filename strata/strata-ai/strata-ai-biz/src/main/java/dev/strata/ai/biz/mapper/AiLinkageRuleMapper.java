// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.mapper;

import dev.strata.ai.api.dto.AiLinkageRuleDto;
import dev.strata.ai.api.request.AiLinkageRuleUpsertRequest;
import dev.strata.ai.biz.entity.AiLinkageRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiLinkageRuleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    AiLinkageRule toEntity(AiLinkageRuleUpsertRequest request);

    @Mapping(target = "id", expression = "java(String.valueOf(rule.getId()))")
    AiLinkageRuleDto toDto(AiLinkageRule rule);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void update(AiLinkageRuleUpsertRequest request, @MappingTarget AiLinkageRule rule);
}
