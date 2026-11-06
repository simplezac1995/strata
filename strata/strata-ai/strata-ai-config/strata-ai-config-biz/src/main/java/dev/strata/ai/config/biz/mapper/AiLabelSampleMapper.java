// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.mapper;

import dev.strata.ai.api.request.AiLabelSampleCreateRequest;
import dev.strata.ai.config.api.dto.AiLabelSampleDto;
import dev.strata.ai.config.api.entity.AiLabelSample;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiLabelSampleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "source", ignore = true)
    AiLabelSample toEntity(AiLabelSampleCreateRequest request);

    AiLabelSampleDto toDto(AiLabelSample sample);
}
