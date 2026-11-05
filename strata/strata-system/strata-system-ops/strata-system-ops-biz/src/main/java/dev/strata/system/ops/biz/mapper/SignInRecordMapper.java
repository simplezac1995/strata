// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper;

import dev.strata.system.api.dto.SignInRecordDto;
import dev.strata.system.ops.api.entity.SignInRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SignInRecordMapper {

    SignInRecordDto toDto(SignInRecord record);
}
