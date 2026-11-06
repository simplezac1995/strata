// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.seed;

import dev.strata.system.api.entity.DemoPayment;
import dev.strata.system.api.entity.DemoProduct;
import dev.strata.system.api.seed.SystemDemoSeedFile;
import dev.strata.system.biz.repository.DemoPaymentRepository;
import dev.strata.system.biz.repository.DemoProductRepository;
import dev.strata.system.communication.api.entity.UserNotificationPref;
import dev.strata.system.communication.biz.repository.UserNotificationPrefRepository;
import dev.strata.system.ops.api.entity.AuditLog;
import dev.strata.system.ops.api.entity.LoginLog;
import dev.strata.system.ops.api.entity.OperationLog;
import dev.strata.system.ops.biz.repository.AuditLogRepository;
import dev.strata.system.ops.biz.repository.LoginLogRepository;
import dev.strata.system.ops.biz.repository.OperationLogRepository;
import dev.strata.system.rbac.api.entity.ApiKey;
import dev.strata.system.rbac.api.entity.Dept;
import dev.strata.system.rbac.api.entity.Menu;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.api.entity.RoleMenu;
import dev.strata.system.rbac.api.entity.Tenant;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.api.entity.UserRole;
import dev.strata.system.rbac.biz.repository.ApiKeyRepository;
import dev.strata.system.rbac.biz.repository.DeptRepository;
import dev.strata.system.rbac.biz.repository.MenuRepository;
import dev.strata.system.rbac.biz.repository.RoleMenuRepository;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import dev.strata.system.rbac.biz.repository.TenantRepository;
import dev.strata.system.rbac.biz.repository.UserRepository;
import dev.strata.system.rbac.biz.repository.UserRoleRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(4)
@ConditionalOnProperty(name = "strata.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class SystemDemoDataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final MenuRepository menuRepository;
    private final TenantRepository tenantRepository;
    private final DeptRepository deptRepository;
    private final DemoProductRepository demoProductRepository;
    private final DemoPaymentRepository demoPaymentRepository;
    private final LoginLogRepository loginLogRepository;
    private final OperationLogRepository operationLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final UserNotificationPrefRepository userNotificationPrefRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    public SystemDemoDataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            RoleMenuRepository roleMenuRepository,
            MenuRepository menuRepository,
            TenantRepository tenantRepository,
            DeptRepository deptRepository,
            DemoProductRepository demoProductRepository,
            DemoPaymentRepository demoPaymentRepository,
            LoginLogRepository loginLogRepository,
            OperationLogRepository operationLogRepository,
            AuditLogRepository auditLogRepository,
            ApiKeyRepository apiKeyRepository,
            UserNotificationPrefRepository userNotificationPrefRepository,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.menuRepository = menuRepository;
        this.tenantRepository = tenantRepository;
        this.deptRepository = deptRepository;
        this.demoProductRepository = demoProductRepository;
        this.demoPaymentRepository = demoPaymentRepository;
        this.loginLogRepository = loginLogRepository;
        this.operationLogRepository = operationLogRepository;
        this.auditLogRepository = auditLogRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.userNotificationPrefRepository = userNotificationPrefRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SystemDemoSeedFile seed = loadSeed();
        Map<String, Role> roles = seedRoles(seed);
        Map<String, User> users = seedUsers(seed);
        seedUserRoles(seed, users, roles);
        seedRoleMenus(seed, roles);
        seedDepartments(seed);
        seedProducts(seed);
        seedPayments(seed);
        seedLoginLogs(seed);
        seedOperationLogs(seed, users);
        seedAuditLogs(seed, users);
        seedApiKeys(seed);
        seedNotificationPreferences(seed, users);
    }

    private SystemDemoSeedFile loadSeed() {
        try (var in = resourceLoader
                .getResource("classpath:seed/system-demo-data.json")
                .getInputStream()) {
            return objectMapper.readValue(in, SystemDemoSeedFile.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load system demo seed data", ex);
        }
    }

    private Map<String, Role> seedRoles(SystemDemoSeedFile seed) {
        Map<String, Role> roles = new HashMap<>();
        for (Role role : roleRepository.findByDeletedFalseOrderByIdAsc()) {
            roles.put(role.getCode(), role);
        }
        for (SystemDemoSeedFile.RoleSeed row : seed.roles()) {
            if (roles.containsKey(row.code())) {
                continue;
            }
            Role role = new Role();
            role.setCode(row.code());
            role.setName(row.name());
            role.setEnabled(row.enabled());
            roles.put(row.code(), roleRepository.save(role));
        }
        return roles;
    }

    private Map<String, User> seedUsers(SystemDemoSeedFile seed) {
        Map<String, User> users = new HashMap<>();
        for (User user : userRepository.findByDeletedFalseOrderByIdAsc()) {
            users.put(user.getUsername(), user);
        }
        Map<String, Long> tenantIds = new HashMap<>();
        for (Tenant tenant : tenantRepository.findAll()) {
            tenantIds.put(tenant.getCode(), tenant.getId());
        }
        for (SystemDemoSeedFile.UserSeed row : seed.users()) {
            User user = users.get(row.username());
            if (user == null) {
                user = new User();
                user.setUsername(row.username());
                user.setPasswordHash(passwordEncoder.encode(row.password()));
                user.setEnabled(row.enabled());
            }
            if (user.getNickname() == null || user.getNickname().isBlank()) {
                user.setNickname(row.nickname());
            }
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                user.setEmail(row.email());
            }
            if (user.getTenantId() == null) {
                user.setTenantId(tenantIds.get(row.tenantCode()));
            }
            users.put(row.username(), userRepository.save(user));
        }
        return users;
    }

    private void seedUserRoles(SystemDemoSeedFile seed, Map<String, User> users, Map<String, Role> roles) {
        Set<String> links = new HashSet<>();
        for (UserRole link : userRoleRepository.findAll()) {
            links.add(link.getUserId() + ":" + link.getRoleId());
        }
        for (SystemDemoSeedFile.UserRoleSeed row : seed.userRoles()) {
            User user = users.get(row.username());
            Role role = roles.get(row.roleCode());
            if (user == null || role == null || !links.add(user.getId() + ":" + role.getId())) {
                continue;
            }
            UserRole link = new UserRole();
            link.setUserId(user.getId());
            link.setRoleId(role.getId());
            userRoleRepository.save(link);
        }
    }

    private void seedRoleMenus(SystemDemoSeedFile seed, Map<String, Role> roles) {
        Map<String, Menu> menus = new HashMap<>();
        for (Menu menu : menuRepository.findAll()) {
            if (menu.getPermission() != null) {
                menus.put(menu.getPermission(), menu);
            }
        }
        Set<String> links = new HashSet<>();
        for (RoleMenu link : roleMenuRepository.findAll()) {
            links.add(link.getRoleId() + ":" + link.getMenuId());
        }
        for (SystemDemoSeedFile.RoleMenuSeed row : seed.roleMenus()) {
            Role role = roles.get(row.roleCode());
            Menu menu = menus.get(row.permission());
            if (role == null || menu == null || !links.add(role.getId() + ":" + menu.getId())) {
                continue;
            }
            RoleMenu link = new RoleMenu();
            link.setRoleId(role.getId());
            link.setMenuId(menu.getId());
            roleMenuRepository.save(link);
        }
    }

    private void seedDepartments(SystemDemoSeedFile seed) {
        Map<String, Dept> departments = new HashMap<>();
        for (Dept department : deptRepository.findAllByOrderBySortOrderAscIdAsc()) {
            departments.put(department.getName(), department);
        }
        for (SystemDemoSeedFile.DepartmentSeed row : seed.departments()) {
            if (departments.containsKey(row.name())) {
                continue;
            }
            Dept department = new Dept();
            department.setName(row.name());
            department.setParentId(
                    row.parentName() == null
                            ? null
                            : departments.get(row.parentName()).getId());
            department.setSortOrder(row.sortOrder());
            department.setEnabled(row.enabled());
            departments.put(row.name(), deptRepository.save(department));
        }
    }

    private void seedProducts(SystemDemoSeedFile seed) {
        for (SystemDemoSeedFile.ProductSeed row : seed.products()) {
            if (demoProductRepository.existsBySku(row.sku())) {
                continue;
            }
            DemoProduct product = new DemoProduct();
            product.setName(row.name());
            product.setSku(row.sku());
            product.setPrice(new BigDecimal(row.price()));
            product.setStock(row.stock());
            product.setDescription(row.description());
            product.setEnabled(row.enabled());
            demoProductRepository.save(product);
        }
    }

    private void seedPayments(SystemDemoSeedFile seed) {
        Set<String> orderNumbers = new HashSet<>();
        for (DemoPayment payment : demoPaymentRepository.findAll()) {
            orderNumbers.add(payment.getOrderNo());
        }
        for (SystemDemoSeedFile.PaymentSeed row : seed.payments()) {
            if (!orderNumbers.add(row.orderNo())) {
                continue;
            }
            DemoPayment payment = new DemoPayment();
            payment.setOrderNo(row.orderNo());
            payment.setAmountCents(row.amountCents());
            payment.setCurrency(row.currency());
            payment.setStatus(row.status());
            payment.setSessionId(row.sessionId());
            demoPaymentRepository.save(payment);
        }
    }

    private void seedLoginLogs(SystemDemoSeedFile seed) {
        if (loginLogRepository.count() > 0) {
            return;
        }
        for (SystemDemoSeedFile.LoginLogSeed row : seed.loginLogs()) {
            LoginLog log = new LoginLog();
            log.setUsername(row.username());
            log.setIp(row.ip());
            log.setLocation(row.location());
            log.setBrowser(row.browser());
            log.setStatus(row.status());
            loginLogRepository.save(log);
        }
    }

    private void seedOperationLogs(SystemDemoSeedFile seed, Map<String, User> users) {
        if (operationLogRepository.count() > 0) {
            return;
        }
        for (SystemDemoSeedFile.OperationLogSeed row : seed.operationLogs()) {
            User user = users.get(row.username());
            OperationLog log = new OperationLog();
            log.setModuleName(row.moduleName());
            log.setActionName(row.actionName());
            log.setOperatorId(user == null ? null : user.getId());
            log.setOperatorName(row.username());
            log.setDetail(row.detail());
            operationLogRepository.save(log);
        }
    }

    private void seedAuditLogs(SystemDemoSeedFile seed, Map<String, User> users) {
        if (auditLogRepository.count() > 0) {
            return;
        }
        for (SystemDemoSeedFile.AuditLogSeed row : seed.auditLogs()) {
            User user = users.get(row.username());
            AuditLog log = new AuditLog();
            log.setEventType(row.eventType());
            log.setModuleName(row.moduleName());
            log.setActionName(row.actionName());
            log.setResourceType(row.resourceType());
            log.setResourceId(row.resourceId());
            log.setOperatorId(user == null ? null : user.getId());
            log.setOperatorName(row.username());
            log.setIpAddress(row.ipAddress());
            log.setUserAgent(row.userAgent());
            log.setSuccess(row.success());
            log.setDetail(row.detail());
            auditLogRepository.save(log);
        }
    }

    private void seedApiKeys(SystemDemoSeedFile seed) {
        Set<String> names = new HashSet<>();
        for (ApiKey apiKey : apiKeyRepository.findByDeletedFalseOrderByIdAsc()) {
            names.add(apiKey.getName());
        }
        for (SystemDemoSeedFile.ApiKeySeed row : seed.apiKeys()) {
            if (!names.add(row.name())) {
                continue;
            }
            ApiKey apiKey = new ApiKey();
            apiKey.setName(row.name());
            apiKey.setKeyPrefix(
                    row.plaintext().substring(0, Math.min(10, row.plaintext().length())));
            apiKey.setKeyHash(DigestUtils.sha256Hex(row.plaintext()));
            apiKey.setScopes(row.scopes());
            apiKey.setEnabled(row.enabled());
            apiKeyRepository.save(apiKey);
        }
    }

    private void seedNotificationPreferences(SystemDemoSeedFile seed, Map<String, User> users) {
        for (SystemDemoSeedFile.NotificationPreferenceSeed row : seed.notificationPreferences()) {
            User user = users.get(row.username());
            if (user == null || userNotificationPrefRepository.existsById(user.getId())) {
                continue;
            }
            UserNotificationPref preference = new UserNotificationPref();
            preference.setUserId(user.getId());
            preference.setEmailEnabled(row.emailEnabled());
            preference.setInAppEnabled(row.inAppEnabled());
            preference.setMarketingEnabled(row.marketingEnabled());
            preference.setSecurityEnabled(row.securityEnabled());
            userNotificationPrefRepository.save(preference);
        }
    }
}
