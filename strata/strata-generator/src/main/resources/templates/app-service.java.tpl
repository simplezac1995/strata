// SPDX-License-Identifier: Apache-2.0
package {{PKG}}.biz.{{MODULE}};

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.data.spec.Specs;
import {{PKG}}.api.dto.{{E}}Dto;
import {{PKG}}.api.dto.{{E}}Query;
import {{PKG}}.api.entity.{{E}};
import {{PKG}}.api.request.{{E}}CreateRequest;
import {{PKG}}.api.request.{{E}}UpdateRequest;
import {{PKG}}.biz.repository.{{E}}Repository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class {{E}}AppService {

    private static final Logger log = LoggerFactory.getLogger({{E}}AppService.class);

    private final {{E}}Repository {{EL}}Repository;
    private final {{E}}Mapper {{EL}}Mapper;

    public {{E}}AppService({{E}}Repository {{EL}}Repository, {{E}}Mapper {{EL}}Mapper) {
        this.{{EL}}Repository = {{EL}}Repository;
        this.{{EL}}Mapper = {{EL}}Mapper;
    }

    @Transactional(readOnly = true)
    public PageResult<{{E}}Dto> page({{E}}Query query) {
        var spec = Specs.of({{E}}.class)
{{SPEC_LINES}}                .toPredicate();
        var pageable = PageRequests.of(query.page(), query.size(), Sort.by(Sort.Direction.DESC, "id"));
        Page<{{E}}> page = {{EL}}Repository.findAll(spec, pageable);
        return PageResult.from(page.map({{EL}}Mapper::toDto));
    }

    @Transactional(readOnly = true)
    public {{E}}Dto get(Long id) {
        return {{EL}}Mapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "{{PERM}}", action = "create")
    public {{E}}Dto create({{E}}CreateRequest request) {
        log.info("Creating {{E}}");
{{CREATE_VALIDATION}}        {{E}} entity = {{EL}}Mapper.toEntity(request);
        {{EL}}Repository.save(entity);
        return {{EL}}Mapper.toDto(entity);
    }

    @Transactional
    @OperationLog(module = "{{PERM}}", action = "update")
    public {{E}}Dto update(Long id, {{E}}UpdateRequest request) {
        log.info("Updating {{E}}");
        {{E}} entity = require(id);
{{UPDATE_VALIDATION}}        {{EL}}Mapper.update(request, entity);
        {{EL}}Repository.save(entity);
        return {{EL}}Mapper.toDto(entity);
    }

    @Transactional
    @OperationLog(module = "{{PERM}}", action = "delete")
    public void delete(Long id) {
        log.info("Deleting {{E}}");
        {{EL}}Repository.delete(require(id));
    }

    @Transactional(readOnly = true)
    public List<{{E}}Dto> list() {
        return {{EL}}Repository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map({{EL}}Mapper::toDto)
                .toList();
    }

    private {{E}} require(Long id) {
        return {{EL}}Repository
                .findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "{{E}} not found"));
    }
}
