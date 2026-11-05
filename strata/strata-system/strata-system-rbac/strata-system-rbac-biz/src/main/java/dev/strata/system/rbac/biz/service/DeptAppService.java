// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.data.tenant.TenantScopeSupport;
import dev.strata.common.security.tenant.TenantProperties;
import dev.strata.system.api.dto.DeptDto;
import dev.strata.system.api.request.DeptCreateRequest;
import dev.strata.system.api.request.DeptUpdateRequest;
import dev.strata.system.rbac.api.entity.Dept;
import dev.strata.system.rbac.biz.mapper.DeptMapper;
import dev.strata.system.rbac.biz.repository.DeptRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeptAppService {

    private static final Logger log = LoggerFactory.getLogger(DeptAppService.class);

    private final DeptRepository deptRepository;
    private final DeptMapper deptMapper;
    private final TenantProperties tenantProperties;

    public DeptAppService(DeptRepository deptRepository, DeptMapper deptMapper, TenantProperties tenantProperties) {
        this.deptRepository = deptRepository;
        this.deptMapper = deptMapper;
        this.tenantProperties = tenantProperties;
    }

    @Transactional(readOnly = true)
    public List<DeptDto> list() {
        return buildTree(deptRepository.findAllByOrderBySortOrderAscIdAsc());
    }

    @Transactional(readOnly = true)
    public List<DeptDto> listChildren(Long parentId) {
        List<Dept> depts = parentId == null
                ? deptRepository.findByParentIdIsNullOrderBySortOrderAscIdAsc()
                : deptRepository.findByParentIdOrderBySortOrderAscIdAsc(parentId);
        return depts.stream().map(deptMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PageResult<DeptDto> pageFlat(String keyword, int page, int size) {
        Page<Dept> result = deptRepository.search(
                keyword, PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "sortOrder", "id")));
        return PageResult.from(result.map(deptMapper::toDto));
    }

    @Transactional(readOnly = true)
    public DeptDto get(Long id) {
        return deptMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "system:dept", action = "create")
    public DeptDto create(DeptCreateRequest request) {
        if (request.parentId() != null && !request.parentId().isBlank()) {
            require(Long.valueOf(request.parentId()));
        }
        Dept dept = deptMapper.toEntity(request);
        TenantScopeSupport.applyTenantIdOnCreate(tenantProperties.isRowLevelEnabled(), dept::setTenantId);
        deptRepository.save(dept);
        return deptMapper.toDto(dept);
    }

    @Transactional
    @OperationLog(module = "system:dept", action = "update")
    public DeptDto update(Long id, DeptUpdateRequest request) {
        Dept dept = require(id);
        Long parentId = blankToNull(request.parentId());
        if (parentId != null) {
            if (parentId.equals(id)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Department cannot be its own parent");
            }
            require(parentId);
        }
        deptMapper.update(request, dept);
        deptRepository.save(dept);
        return deptMapper.toDto(dept);
    }

    @Transactional
    @OperationLog(module = "system:dept", action = "delete")
    public void delete(Long id) {
        require(id);
        if (deptRepository.countByParentId(id) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "Department has children");
        }
        deptRepository.deleteById(id);
    }

    List<DeptDto> buildTree(List<Dept> depts) {
        Map<Long, DeptDto> map = depts.stream().collect(Collectors.toMap(Dept::getId, deptMapper::toDto));
        List<DeptDto> roots = new ArrayList<>();
        for (DeptDto dto : map.values()) {
            if (dto.parentId() == null) {
                roots.add(dto);
            } else {
                DeptDto parent = map.get(Long.valueOf(dto.parentId()));
                if (parent != null) {
                    parent.children().add(dto);
                } else {
                    roots.add(dto);
                }
            }
        }
        sortTree(roots);
        return roots;
    }

    private static void sortTree(List<DeptDto> nodes) {
        nodes.sort(Comparator.comparingInt(DeptDto::sortOrder).thenComparing(DeptDto::id));
        for (DeptDto node : nodes) {
            sortTree(node.children());
        }
    }

    private Dept require(Long id) {
        return deptRepository.findById(id).orElseThrow(() -> new NotFoundException("Department not found"));
    }

    private static Long blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }
}
