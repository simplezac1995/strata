// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.data.spec.Specs;
import dev.strata.common.data.tenant.TenantScopeSupport;
import dev.strata.common.security.audit.AuditLog;
import dev.strata.common.security.tenant.TenantProperties;
import dev.strata.system.api.dto.RoleDto;
import dev.strata.system.api.request.RoleCreateRequest;
import dev.strata.system.api.request.RoleUpdateRequest;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.api.entity.RoleMenu;
import dev.strata.system.rbac.biz.mapper.RoleMapper;
import dev.strata.system.rbac.biz.metrics.RbacMetrics;
import dev.strata.system.rbac.biz.repository.RoleMenuRepository;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import dev.strata.system.rbac.biz.repository.UserRoleRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleAppService {

    private static final Logger log = LoggerFactory.getLogger(RoleAppService.class);

    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMapper roleMapper;
    private final TenantProperties tenantProperties;
    private final RbacMetrics rbacMetrics;

    public RoleAppService(
            RoleRepository roleRepository,
            RoleMenuRepository roleMenuRepository,
            UserRoleRepository userRoleRepository,
            RoleMapper roleMapper,
            TenantProperties tenantProperties,
            RbacMetrics rbacMetrics) {
        this.roleRepository = roleRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleMapper = roleMapper;
        this.tenantProperties = tenantProperties;
        this.rbacMetrics = rbacMetrics;
    }

    @Transactional(readOnly = true)
    public List<RoleDto> list() {
        List<Role> roles = roleRepository.findByDeletedFalseOrderByIdAsc();
        Map<Long, List<String>> menusByRole =
                menuIdsByRole(roles.stream().map(Role::getId).toList());
        return roles.stream()
                .map(role -> roleMapper.toDto(role, menusByRole.getOrDefault(role.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<RoleDto> page(String keyword, int page, int size, String sortBy, String sortDir) {
        Specification<Role> spec =
                Specs.of(Role.class).eq("deleted", false).toSpecification().and(keywordSpec(keyword));
        Page<Role> result =
                roleRepository.findAll(spec, PageRequests.of(page, size, PageRequests.parseSort(sortBy, sortDir)));
        Map<Long, List<String>> menusByRole =
                menuIdsByRole(result.getContent().stream().map(Role::getId).toList());
        return PageResult.from(
                result.map(role -> roleMapper.toDto(role, menusByRole.getOrDefault(role.getId(), List.of()))));
    }

    private static Specification<Role> keywordSpec(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate code = cb.like(cb.lower(root.get("code")), pattern);
            Predicate name = cb.like(cb.lower(root.get("name")), pattern);
            return cb.or(code, name);
        };
    }

    @Transactional(readOnly = true)
    public RoleDto get(Long id) {
        Role role = require(id);
        return roleMapper.toDto(role, menuIds(role.getId()));
    }

    @OperationLog(module = "system:role", action = "create")
    @Transactional
    @AuditLog(action = "ROLE_CREATE", description = "Create new role", category = "ROLE_MANAGEMENT")
    public RoleDto create(RoleCreateRequest request) {
        if (roleRepository.findByCodeAndDeletedFalse(request.code()).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Role code already exists");
        }
        Role role = roleMapper.toEntity(request);
        TenantScopeSupport.applyTenantIdOnCreate(tenantProperties.isRowLevelEnabled(), role::setTenantId);
        roleRepository.save(role);
        replaceMenus(role.getId(), request.menuIds());
        rbacMetrics.recordRoleCreated();
        return roleMapper.toDto(role, menuIds(role.getId()));
    }

    @OperationLog(module = "system:role", action = "update")
    @Transactional
    @AuditLog(action = "ROLE_UPDATE", description = "Update role", category = "ROLE_MANAGEMENT")
    public RoleDto update(Long id, RoleUpdateRequest request) {
        Role role = require(id);
        roleMapper.update(request, role);
        roleRepository.save(role);
        if (request.menuIds() != null) {
            replaceMenus(role.getId(), request.menuIds());
        }
        rbacMetrics.recordRoleUpdated();
        return roleMapper.toDto(role, menuIds(role.getId()));
    }

    @OperationLog(module = "system:role", action = "delete")
    @Transactional
    @AuditLog(action = "ROLE_DELETE", description = "Delete role", sensitive = true, category = "ROLE_MANAGEMENT")
    public void delete(Long id) {
        Role role = require(id);
        role.setDeleted(true);
        role.setEnabled(false);
        roleRepository.save(role);
        /* Revoke immediately: drop user and menu links so no stale grants remain. */
        userRoleRepository.deleteByRoleId(id);
        roleMenuRepository.deleteByRoleId(id);
        rbacMetrics.recordRoleDeleted();
    }

    private Role require(Long id) {
        return roleRepository
                .findById(id)
                .filter(role -> !role.isDeleted())
                .orElseThrow(() -> new NotFoundException("Role not found"));
    }

    private List<String> menuIds(Long roleId) {
        return roleMenuRepository.findByRoleId(roleId).stream()
                .map(link -> String.valueOf(link.getMenuId()))
                .toList();
    }

    private Map<Long, List<String>> menuIdsByRole(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return roleMenuRepository.findByRoleIdIn(roleIds).stream()
                .collect(Collectors.groupingBy(
                        RoleMenu::getRoleId,
                        Collectors.mapping(link -> String.valueOf(link.getMenuId()), Collectors.toList())));
    }

    private void replaceMenus(Long roleId, List<String> menuIds) {
        roleMenuRepository.deleteByRoleId(roleId);
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        List<RoleMenu> links = menuIds.stream()
                .map(menuId -> {
                    RoleMenu link = new RoleMenu();
                    link.setRoleId(roleId);
                    link.setMenuId(Long.valueOf(menuId));
                    return link;
                })
                .toList();
        roleMenuRepository.saveAll(links);
    }
}
