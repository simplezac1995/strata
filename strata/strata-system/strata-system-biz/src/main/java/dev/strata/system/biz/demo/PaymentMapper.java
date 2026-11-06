// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.demo;

import dev.strata.system.api.dto.PaymentDto;
import dev.strata.system.api.entity.DemoPayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    @Mapping(target = "id", expression = "java(String.valueOf(payment.getId()))")
    PaymentDto toDto(DemoPayment payment);
}
