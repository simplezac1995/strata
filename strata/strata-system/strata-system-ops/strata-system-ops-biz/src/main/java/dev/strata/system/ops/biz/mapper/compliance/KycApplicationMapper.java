// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper.compliance;

import dev.strata.system.api.dto.compliance.KycApplicationDto;
import dev.strata.system.api.request.compliance.KycSubmitRequest;
import dev.strata.system.ops.api.entity.compliance.KycApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface KycApplicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "reviewerId", ignore = true)
    @Mapping(target = "rejectReason", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    KycApplication toEntity(KycSubmitRequest request);

    @Mapping(target = "id", expression = "java(String.valueOf(row.getId()))")
    @Mapping(target = "userId", expression = "java(row.getUserId() == null ? null : String.valueOf(row.getUserId()))")
    @Mapping(
            target = "reviewerId",
            expression = "java(row.getReviewerId() == null ? null : String.valueOf(row.getReviewerId()))")
    KycApplicationDto toDto(KycApplication row);
}
