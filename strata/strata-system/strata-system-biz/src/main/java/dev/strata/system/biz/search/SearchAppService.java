// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.search;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.system.api.dto.SearchResultDto;
import dev.strata.system.api.dto.SearchResultsDto;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import dev.strata.system.rbac.biz.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-entity global search (#15) over staff-side entities. Orders live in the casino wallet domain, which
 * the staff module must not depend on directly (ADR-0026); they are always empty pending a read contract.
 */
@Service
public class SearchAppService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    /** Static catalogue of registered front-end pages; not queried from the DB. */
    private static final List<SearchResultDto> PAGES = List.of(
            new SearchResultDto("dashboard", "仪表盘", "Overview", "/dashboard"),
            new SearchResultDto("users", "用户管理", "Staff users", "/system/users"),
            new SearchResultDto("roles", "角色管理", "Roles & permissions", "/system/roles"),
            new SearchResultDto("menus", "菜单管理", "Menus", "/system/menus"),
            new SearchResultDto("ops-logs", "操作日志", "Audit trail", "/system/operation-logs"),
            new SearchResultDto("monitor", "系统监控", "JVM & pool snapshot", "/system/monitor"));

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public SearchAppService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public SearchResultsDto search(String q, int limit) {
        int effective = clampLimit(limit);
        if (q == null || q.isBlank()) {
            return new SearchResultsDto(List.of(), List.of(), List.of(), PAGES);
        }
        String keyword = q.trim();
        PageRequest page = PageRequest.of(0, effective);

        List<SearchResultDto> users = userRepository.search(keyword, page).stream()
                .map(SearchAppService::toUserResult)
                .toList();
        List<SearchResultDto> roles = roleRepository.search(keyword, page).stream()
                .map(SearchAppService::toRoleResult)
                .toList();
        // TODO(system-search): orders come from casino payment intents; empty pending a staff read contract.
        return new SearchResultsDto(users, roles, List.of(), PAGES);
    }

    private static int clampLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        if (limit > MAX_LIMIT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "limit must be <= " + MAX_LIMIT);
        }
        return limit;
    }

    private static SearchResultDto toUserResult(User user) {
        String label =
                user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname();
        return new SearchResultDto(String.valueOf(user.getId()), label, user.getUsername(), "/system/users");
    }

    private static SearchResultDto toRoleResult(Role role) {
        return new SearchResultDto(String.valueOf(role.getId()), role.getName(), role.getCode(), "/system/roles");
    }
}
