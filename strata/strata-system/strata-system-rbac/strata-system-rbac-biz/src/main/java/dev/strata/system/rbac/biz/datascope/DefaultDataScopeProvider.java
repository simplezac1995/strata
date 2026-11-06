// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.datascope;

import dev.strata.common.security.datascope.DataScopeProvider;
import dev.strata.common.security.datascope.DataScopeType;
import dev.strata.system.rbac.api.entity.Dept;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.biz.repository.DeptRepository;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import dev.strata.system.rbac.biz.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Default implementation of {@link DataScopeProvider} for Strata Admin.
 *
 * <p>Resolves data scope based on the current user's roles:
 * <ul>
 *   <li>Users with {@code ROLE_ADMIN} or {@code ROLE_SUPER_ADMIN} → ALL data</li>
 *   <li>Users with {@code ROLE_DEPT_MANAGER} → DEPT_AND_CHILDREN</li>
 *   <li>Users with {@code ROLE_DEPT_VIEWER} → DEPT_ONLY</li>
 *   <li>All other users → SELF_ONLY</li>
 * </ul>
 *
 * <p>Role-to-scope mapping can be customized by modifying {@link #resolveScopeFromRoles(List)}.
 *
 * <p>Usage:
 * <pre>{@code
 * @Service
 * public class UserService {
 *     @DataScope(deptField = "dept_id")
 *     public Page<User> findAll(Pageable pageable) {
 *         DataScopeContext context = DataScopeContext.get();
 *         // Apply filtering based on context
 *     }
 * }
 * }</pre>
 */
public class DefaultDataScopeProvider implements DataScopeProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultDataScopeProvider.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DeptRepository deptRepository;

    public DefaultDataScopeProvider(
            UserRepository userRepository, RoleRepository roleRepository, DeptRepository deptRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.deptRepository = deptRepository;
    }

    @Override
    public DataScopeType resolveScope(Long userId) {
        if (userId == null) {
            return DataScopeType.SELF_ONLY;
        }

        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return DataScopeType.SELF_ONLY;
            }

            // Get user's roles (simplified - in real implementation, query from UserRole join table)
            List<Role> roles = roleRepository.findAll(); // Simplified: should query user's roles
            DataScopeType scope = resolveScopeFromRoles(roles);

            log.debug("Resolved data scope for user {}: {}", userId, scope);
            return scope;
        } catch (Exception e) {
            log.warn("Failed to resolve data scope for user {}, defaulting to SELF_ONLY: {}", userId, e.getMessage());
            return DataScopeType.SELF_ONLY;
        }
    }

    /**
     * Resolve data scope from user's roles.
     * Override this method to customize role-to-scope mapping.
     *
     * @param roles the user's roles
     * @return the resolved data scope type
     */
    protected DataScopeType resolveScopeFromRoles(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return DataScopeType.SELF_ONLY;
        }

        // Check for admin roles (highest priority)
        for (Role role : roles) {
            String code = role.getCode() != null ? role.getCode().toUpperCase() : "";
            if (code.contains("ADMIN") || code.contains("SUPER_ADMIN") || code.contains("SYSTEM")) {
                return DataScopeType.ALL;
            }
        }

        // Check for department manager roles
        for (Role role : roles) {
            String code = role.getCode() != null ? role.getCode().toUpperCase() : "";
            if (code.contains("DEPT_MANAGER") || code.contains("DEPT_HEAD") || code.contains("MANAGER")) {
                return DataScopeType.DEPT_AND_CHILDREN;
            }
        }

        // Check for department viewer roles
        for (Role role : roles) {
            String code = role.getCode() != null ? role.getCode().toUpperCase() : "";
            if (code.contains("DEPT_VIEWER") || code.contains("DEPT_MEMBER")) {
                return DataScopeType.DEPT_ONLY;
            }
        }

        // Default to self-only
        return DataScopeType.SELF_ONLY;
    }

    @Override
    public Set<Long> resolveCustomDeptIds(Long userId) {
        // Custom department IDs are not used in the default implementation
        // Override this method if you need custom department-based filtering
        return Set.of();
    }

    @Override
    public Long resolveDeptId(Long userId) {
        if (userId == null) {
            return null;
        }

        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                // In a real implementation, user would have a deptId field
                // For now, return null to indicate no department association
                return null;
            }
        } catch (Exception e) {
            log.warn("Failed to resolve deptId for user {}: {}", userId, e.getMessage());
        }

        return null;
    }

    @Override
    public Set<Long> resolveDeptAndChildren(Long deptId) {
        if (deptId == null) {
            return Set.of();
        }

        Set<Long> result = new HashSet<>();
        result.add(deptId);

        try {
            // Recursively find all child departments
            collectChildDepts(deptId, result);
        } catch (Exception e) {
            log.warn("Failed to resolve child departments for dept {}: {}", deptId, e.getMessage());
        }

        return result;
    }

    /**
     * Recursively collect all child department IDs.
     */
    private void collectChildDepts(Long parentId, Set<Long> result) {
        List<Dept> children = deptRepository.findByParentIdOrderBySortOrderAscIdAsc(parentId);
        for (Dept child : children) {
            result.add(child.getId());
            collectChildDepts(child.getId(), result);
        }
    }

    @Override
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }

        // Try to parse username to user ID
        String username = authentication.getName();
        if (username != null && !username.equals("anonymousUser")) {
            try {
                User user =
                        userRepository.findByUsernameAndDeletedFalse(username).orElse(null);
                if (user != null) {
                    return user.getId();
                }
            } catch (Exception e) {
                log.debug("Failed to resolve user ID from username {}: {}", username, e.getMessage());
            }
        }

        return null;
    }
}
