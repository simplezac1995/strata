// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.tenant.TenantContext;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.data.spec.Specs;
import dev.strata.common.security.audit.AuditLog;
import dev.strata.common.security.datascope.DataScope;
import dev.strata.common.security.datascope.DataScopeSpecification;
import dev.strata.common.security.tenant.TenantProperties;
import dev.strata.plugin.api.spreadsheet.SpreadsheetExporter;
import dev.strata.system.api.dto.UserDto;
import dev.strata.system.api.request.UserCreateRequest;
import dev.strata.system.api.request.UserUpdateRequest;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.api.entity.UserRole;
import dev.strata.system.rbac.biz.mapper.UserMapper;
import dev.strata.system.rbac.biz.metrics.RbacMetrics;
import dev.strata.system.rbac.biz.repository.UserRepository;
import dev.strata.system.rbac.biz.repository.UserRoleRepository;
import dev.strata.system.rbac.biz.service.support.UserTenantSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAppService {

    private static final Logger log = LoggerFactory.getLogger(UserAppService.class);

    /** Hard ceiling on one xlsx export so a full-table export cannot OOM the heap (P1-14). */
    private static final int EXPORT_ROW_CAP = 10_000;

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final TenantProperties tenantProperties;
    private final Optional<SpreadsheetExporter> spreadsheetExporter;
    private final RbacMetrics rbacMetrics;

    public UserAppService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            TenantProperties tenantProperties,
            Optional<SpreadsheetExporter> spreadsheetExporter,
            RbacMetrics rbacMetrics) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.tenantProperties = tenantProperties;
        this.spreadsheetExporter = spreadsheetExporter;
        this.rbacMetrics = rbacMetrics;
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        Specification<User> spec = baseSpec();
        List<User> users = userRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id"));
        Map<Long, List<String>> rolesByUser =
                roleIdsByUser(users.stream().map(User::getId).toList());
        return users.stream()
                .map(user -> userMapper.toDto(user, rolesByUser.getOrDefault(user.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDto get(Long id) {
        User user = require(id);
        return userMapper.toDto(user, roleIds(user.getId()));
    }

    /**
     * Self-service email change. This batch writes the new address directly and flips {@code emailVerified}
     * back to {@code false}; no verification mail is sent yet (TODO: wire the email verification flow).
     */
    @OperationLog(module = "system:user", action = "update")
    @Transactional
    public UserDto updateMyEmail(Long userId, String email) {
        User user = require(userId);
        if (userRepository.existsByEmail(email) && !email.equals(user.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED, "Email already registered");
        }
        user.setEmail(email);
        user.setEmailVerified(false);
        userRepository.save(user);
        return userMapper.toDto(user, roleIds(user.getId()));
    }

    /**
     * Self-service phone binding. This batch stores the number directly without an SMS OTP (TODO: gate on an
     * SMS verification code before production trust).
     */
    @OperationLog(module = "system:user", action = "update")
    @Transactional
    public UserDto updateMyPhone(Long userId, String phone) {
        User user = require(userId);
        user.setPhone(phone);
        userRepository.save(user);
        return userMapper.toDto(user, roleIds(user.getId()));
    }

    /** Self-service avatar update: the URL comes from the existing file upload endpoint. */
    @OperationLog(module = "system:user", action = "update")
    @Transactional
    public UserDto updateMyAvatar(Long userId, String avatar) {
        User user = require(userId);
        user.setAvatar(avatar);
        userRepository.save(user);
        return userMapper.toDto(user, roleIds(user.getId()));
    }

    @Transactional(readOnly = true)
    @DataScope(userField = "created_by", description = "User list filtered by data scope")
    public PageResult<UserDto> page(String keyword, int page, int size, String sortBy, String sortDir) {
        Specification<User> spec = baseSpec().and(keywordSpec(keyword)).and(DataScopeSpecification.apply());
        Page<User> result =
                userRepository.findAll(spec, PageRequests.of(page, size, PageRequests.parseSort(sortBy, sortDir)));
        Map<Long, List<String>> rolesByUser =
                roleIdsByUser(result.getContent().stream().map(User::getId).toList());
        return PageResult.from(
                result.map(user -> userMapper.toDto(user, rolesByUser.getOrDefault(user.getId(), List.of()))));
    }

    @Transactional(readOnly = true)
    public byte[] exportXlsx(String keyword) {
        SpreadsheetExporter exporter =
                spreadsheetExporter.orElseThrow(() -> new NotFoundException("Spreadsheet exporter not configured"));
        // P1-14: push the keyword filter into the Specification and page through the result set instead of loading
        // every user into memory then filtering in Java. A 10k row cap keeps a runaway export bounded.
        Specification<User> spec = baseSpec().and(keywordSpec(keyword));
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        List<List<String>> rows = new ArrayList<>();
        int page = 0;
        while (rows.size() < EXPORT_ROW_CAP) {
            Page<User> result = userRepository.findAll(spec, PageRequests.of(page, 1_000, sort));
            for (User u : result.getContent()) {
                rows.add(List.of(
                        String.valueOf(u.getId()),
                        u.getUsername(),
                        u.getNickname() == null ? "" : u.getNickname(),
                        String.valueOf(u.isEnabled())));
            }
            if (!result.hasNext()) {
                break;
            }
            page++;
        }
        List<String> headers = List.of("id", "username", "nickname", "enabled");
        return exporter.writeXlsx("users", headers, rows);
    }

    @OperationLog(module = "system:user", action = "create")
    @Transactional
    @AuditLog(action = "USER_CREATE", description = "Create new user", category = "USER_MANAGEMENT")
    public UserDto create(UserCreateRequest request) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.username())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Username already exists");
        }
        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        UserTenantSupport.applyTenantOnCreate(user, tenantProperties);
        userRepository.save(user);
        replaceRoles(user.getId(), request.roleIds());
        rbacMetrics.recordUserCreated();
        return userMapper.toDto(user, roleIds(user.getId()));
    }

    @OperationLog(module = "system:user", action = "update")
    @Transactional
    @AuditLog(action = "USER_UPDATE", description = "Update user", category = "USER_MANAGEMENT")
    public UserDto update(Long id, UserUpdateRequest request) {
        User user = require(id);
        userMapper.update(request, user);
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        userRepository.save(user);
        if (request.roleIds() != null) {
            replaceRoles(user.getId(), request.roleIds());
        }
        rbacMetrics.recordUserUpdated();
        return userMapper.toDto(user, roleIds(user.getId()));
    }

    @OperationLog(module = "system:user", action = "delete")
    @Transactional
    @AuditLog(action = "USER_DELETE", description = "Delete user", sensitive = true, category = "USER_MANAGEMENT")
    public void delete(Long id) {
        User user = require(id);
        user.setDeleted(true);
        user.setEnabled(false);
        userRepository.save(user);
        rbacMetrics.recordUserDeleted();
    }

    /**
     * GDPR right to erasure / data minimization: irreversibly pseudonymizes a staff account in place. The row
     * itself is kept so transaction history and audit FK references stay intact (compliance requires retaining
     * historical records), but all directly identifying fields are scrubbed:
     *
     * <ul>
     *   <li>username -> {@code deleted_<id>} (deterministic, stays unique on the username column)
     *   <li>email -> {@code deleted_<id>@anonymized.local}
     *   <li>nickname / phone / avatar -> cleared
     *   <li>password-reset token and TOTP secret are dropped so no residual credential material survives
     *   <li>account disabled and soft-deleted so it can no longer authenticate or appear in lists
     * </ul>
     *
     * This is an in-place UPDATE only; no transaction / wallet / player history is touched.
     */
    @OperationLog(module = "system:user", action = "delete")
    @Transactional
    @AuditLog(
            action = "USER_ANONYMIZE",
            description = "Anonymize user (GDPR erasure)",
            sensitive = true,
            category = "USER_MANAGEMENT")
    public void anonymize(Long id) {
        User user = require(id);
        String stub = "deleted_" + user.getId();
        user.setUsername(stub);
        user.setEmail(stub + "@anonymized.local");
        user.setEmailVerified(false);
        user.setNickname(null);
        user.setPhone(null);
        user.setAvatar(null);
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setTotpSecret(null);
        user.setTotpEnabled(false);
        user.setEnabled(false);
        user.setDeleted(true);
        userRepository.save(user);
        rbacMetrics.recordUserDeleted();
    }

    @OperationLog(module = "system:user", action = "delete")
    @Transactional
    @AuditLog(
            action = "USER_DELETE_BATCH",
            description = "Batch delete users",
            sensitive = true,
            category = "USER_MANAGEMENT")
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        // P2-21 + tenant isolation: resolve which of these ids are actually visible in the current tenant (not
        // deleted, tenant-scoped via baseSpec) before issuing one bulk UPDATE. An id that is missing, already
        // deleted, or belongs to another tenant makes the batch fail with NOT_FOUND — matching the per-row
        // require() semantics — so a caller cannot soft-delete across tenants by guessing ids.
        List<Long> distinctIds = ids.stream().distinct().toList();
        Specification<User> visible =
                baseSpec().and(Specs.of(User.class).in("id", distinctIds).toSpecification());
        List<User> found = userRepository.findAll(visible);
        if (found.size() != distinctIds.size()) {
            throw new NotFoundException("User not found");
        }
        List<Long> visibleIds = found.stream().map(User::getId).toList();
        userRepository.softDeleteByIds(visibleIds);
        rbacMetrics.recordUserDeleted();
    }

    private Specification<User> baseSpec() {
        return Specs.of(User.class)
                .eq("deleted", false)
                .toSpecification()
                .and(UserTenantSupport.tenantScope(tenantProperties));
    }

    private Specification<User> keywordSpec(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return Specs.of(User.class)
                .like("username", keyword)
                .toSpecification()
                .or(Specs.of(User.class).like("nickname", keyword).toSpecification());
    }

    private User require(Long id) {
        User user = userRepository
                .findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (tenantProperties.isRowLevelEnabled()) {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId != null
                    && user.getTenantId() != null
                    && !user.getTenantId().equals(tenantId)) {
                throw new NotFoundException("User not found");
            }
        }
        return user;
    }

    private List<String> roleIds(Long userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(link -> String.valueOf(link.getRoleId()))
                .toList();
    }

    private Map<Long, List<String>> roleIdsByUser(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRoleRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(
                        UserRole::getUserId,
                        Collectors.mapping(link -> String.valueOf(link.getRoleId()), Collectors.toList())));
    }

    private void replaceRoles(Long userId, List<String> roleIds) {
        userRoleRepository.deleteByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<UserRole> links = roleIds.stream()
                .map(roleId -> {
                    UserRole link = new UserRole();
                    link.setUserId(userId);
                    link.setRoleId(Long.valueOf(roleId));
                    return link;
                })
                .toList();
        userRoleRepository.saveAll(links);
    }
}
