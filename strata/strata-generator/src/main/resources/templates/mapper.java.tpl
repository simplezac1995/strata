// SPDX-License-Identifier: Apache-2.0
package {{PKG}}.biz.{{MODULE}};

import {{PKG}}.api.dto.{{E}}Dto;
import {{PKG}}.api.entity.{{E}};
import {{PKG}}.api.request.{{E}}CreateRequest;
import {{PKG}}.api.request.{{E}}UpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface {{E}}Mapper {

    @Mapping(target = "id", ignore = true)
    {{E}} toEntity({{E}}CreateRequest request);

    {{E}}Dto toDto({{E}} entity);

    @Mapping(target = "id", ignore = true)
    void update({{E}}UpdateRequest request, @MappingTarget {{E}} entity);
}
