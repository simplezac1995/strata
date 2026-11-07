// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.seed;

import dev.strata.system.api.seed.OpsMenuSeedContributor;
import dev.strata.system.communication.api.entity.MessageChannel;
import dev.strata.system.communication.biz.repository.MessageChannelRepository;
import dev.strata.system.config.api.entity.I18nEntry;
import dev.strata.system.config.biz.repository.I18nEntryRepository;
import dev.strata.system.ops.api.entity.SysJob;
import dev.strata.system.ops.biz.repository.SysJobRepository;
import dev.strata.system.rbac.api.entity.Menu;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.api.entity.RoleMenu;
import dev.strata.system.rbac.biz.repository.MenuRepository;
import dev.strata.system.rbac.biz.repository.RoleMenuRepository;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds ops/settings demo rows and ensures admin menus/permissions exist for new domains.
 * Safe to re-run: skips when data or permission markers already exist.
 */
@Component
@ConditionalOnProperty(name = "strata.seed.enabled", havingValue = "true", matchIfMissing = true)
@Order(3)
public class OpsDataInitializer implements ApplicationRunner {

    private final I18nEntryRepository i18nEntryRepository;
    private final SysJobRepository sysJobRepository;
    private final MessageChannelRepository messageChannelRepository;
    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final List<OpsMenuSeedContributor> menuSeedContributors;
    private final OpsMenuSeedSupport menuSeedSupport;

    public OpsDataInitializer(
            I18nEntryRepository i18nEntryRepository,
            SysJobRepository sysJobRepository,
            MessageChannelRepository messageChannelRepository,
            MenuRepository menuRepository,
            RoleRepository roleRepository,
            RoleMenuRepository roleMenuRepository,
            List<OpsMenuSeedContributor> menuSeedContributors,
            OpsMenuSeedSupport menuSeedSupport) {
        this.i18nEntryRepository = i18nEntryRepository;
        this.sysJobRepository = sysJobRepository;
        this.messageChannelRepository = messageChannelRepository;
        this.menuRepository = menuRepository;
        this.roleRepository = roleRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.menuSeedContributors = menuSeedContributors;
        this.menuSeedSupport = menuSeedSupport;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedI18n();
        seedJobs();
        seedMessageChannels();
        seedMenusIfMissing();
        seedAuditLogMenuIfMissing();
        menuSeedContributors.forEach(contributor -> contributor.seed(menuSeedSupport));
    }

    private void seedI18n() {
        if (i18nEntryRepository.count() > 0) {
            return;
        }
        String[][] rows = {
            {"router.dashboard", "首页", "Dashboard"},
            {"router.system", "系统管理", "System"},
            {"router.users", "用户管理", "Users"},
            {"router.roles", "角色管理", "Roles"},
            {"router.menus", "菜单管理", "Menus"},
            {"router.settings", "站点设置", "Site Settings"},
            {"router.i18n", "国际化", "Internationalization"},
            {"common.save", "保存", "Save"},
            {"common.cancel", "取消", "Cancel"},
            {"common.delete", "删除", "Delete"},
            {"common.search", "搜索", "Search"},
            {"common.create", "新建", "Create"},
            {"common.edit", "编辑", "Edit"},
            {"nav.notifications", "通知中心", "Notifications"},
            {"nav.api.keys", "API 密钥", "API Keys"},
        };
        for (String[] row : rows) {
            I18nEntry entry = new I18nEntry();
            entry.setMessageKey(row[0]);
            entry.setZh(row[1]);
            entry.setEn(row[2]);
            i18nEntryRepository.save(entry);
        }
    }

    private void seedJobs() {
        if (sysJobRepository.count() > 0) {
            return;
        }
        sysJobRepository.save(job("Cleanup Temp Files", "SYSTEM", "0 0 2 * * ?", "PAUSED", true));
        sysJobRepository.save(job("Sync Login Stats", "REPORT", "0 */30 * * * ?", "PAUSED", true));
        sysJobRepository.save(job("Expire API Tokens", "SECURITY", "0 15 * * * ?", "PAUSED", false));
    }

    private static SysJob job(String name, String group, String cron, String status, boolean enabled) {
        SysJob job = new SysJob();
        job.setName(name);
        job.setJobGroup(group);
        job.setCron(cron);
        job.setStatus(status);
        job.setEnabled(enabled);
        return job;
    }

    private void seedMessageChannels() {
        if (messageChannelRepository.count() > 0) {
            return;
        }
        for (String type : List.of("email", "sms", "webhook", "internal")) {
            MessageChannel channel = new MessageChannel();
            channel.setChannelType(type);
            channel.setConfigJson("{}");
            channel.setEnabled(false);
            messageChannelRepository.save(channel);
        }
    }

    private void seedMenusIfMissing() {
        if (menuRepository.existsByPermission("system:sitesetting:list")) {
            return;
        }

        Menu system =
                menuRepository.findFirstByPathAndType("/system", "directory").orElse(null);
        Menu tools = menuRepository
                .findFirstByPathAndType("/tools", "directory")
                .orElseGet(() -> {
                    Menu dir = menu("Tools", "directory", "/tools", "Layout", null, null, 6);
                    return menuRepository.save(dir);
                });
        Menu settings = menuRepository
                .findFirstByPathAndType("/settings", "directory")
                .orElseGet(() -> {
                    Menu dir = menu("Settings", "directory", "/settings", "Layout", null, null, 7);
                    return menuRepository.save(dir);
                });

        List<Menu> created = new ArrayList<>();
        if (system != null) {
            created.addAll(menuWithButtons(
                    "Site Settings", "site-settings", "system/SiteSettings", "system:sitesetting", system.getId(), 19));
            created.addAll(menuWithButtons("I18n", "i18n", "system/I18n", "system:i18n", system.getId(), 20));
        }

        /* Tools: cache / schedules / message-push — paths match App.tsx */
        Menu cacheParent =
                menu("Cache Monitor", "menu", "cache", "tools/CacheMonitor", "system:cache:list", tools.getId(), 1);
        menuRepository.save(cacheParent);
        created.add(cacheParent);
        Menu cacheDelete = button("Cache Delete", "system:cache:delete", cacheParent.getId(), 1);
        menuRepository.save(cacheDelete);
        created.add(cacheDelete);

        created.addAll(menuWithButtons("Schedules", "schedules", "tools/Schedules", "system:job", tools.getId(), 2));
        created.addAll(menuWithButtons(
                "Message Push", "message-push", "tools/MessagePush", "system:messagechannel", tools.getId(), 3));

        created.addAll(
                menuWithButtons("API Keys", "api-keys", "settings/ApiKeys", "system:apikey", settings.getId(), 1));

        roleRepository.findByCodeAndDeletedFalse("admin").ifPresent(admin -> linkMenus(admin, created));
    }

    private void seedAuditLogMenuIfMissing() {
        if (menuRepository.existsByPermission("system:auditlog:list")) {
            return;
        }
        Menu system =
                menuRepository.findFirstByPathAndType("/system", "directory").orElse(null);
        if (system == null) {
            return;
        }
        Menu auditMenu = menu(
                "Audit Logs",
                "menu",
                "logs/audit",
                "system/logs/AuditLogs",
                "system:auditlog:list",
                system.getId(),
                21);
        menuRepository.save(auditMenu);
        roleRepository.findByCodeAndDeletedFalse("admin").ifPresent(admin -> linkMenus(admin, List.of(auditMenu)));
    }

    private void linkMenus(Role admin, List<Menu> menus) {
        for (Menu menu : menus) {
            RoleMenu link = new RoleMenu();
            link.setRoleId(admin.getId());
            link.setMenuId(menu.getId());
            roleMenuRepository.save(link);
        }
    }

    private List<Menu> menuWithButtons(
            String name, String path, String component, String permission, Long parentId, int sort) {
        Menu menu = menu(name, "menu", path, component, permission + ":list", parentId, sort);
        menuRepository.save(menu);
        List<Menu> all = new ArrayList<>();
        all.add(menu);
        if ("system:sitesetting".equals(permission) || "system:messagechannel".equals(permission)) {
            Menu update = button(name + " Update", permission + ":update", menu.getId(), 1);
            menuRepository.save(update);
            all.add(update);
            return all;
        }
        if ("system:cache".equals(permission)) {
            Menu delete = button(name + " Delete", permission + ":delete", menu.getId(), 1);
            menuRepository.save(delete);
            all.add(delete);
            return all;
        }
        Menu create = button(name + " Create", permission + ":create", menu.getId(), 1);
        Menu update = button(name + " Update", permission + ":update", menu.getId(), 2);
        Menu delete = button(name + " Delete", permission + ":delete", menu.getId(), 3);
        menuRepository.saveAll(List.of(create, update, delete));
        all.add(create);
        all.add(update);
        all.add(delete);
        return all;
    }

    private static Menu menu(
            String name, String type, String path, String component, String permission, Long parentId, int sort) {
        Menu menu = new Menu();
        menu.setName(name);
        menu.setType(type);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setPermission(permission);
        menu.setParentId(parentId);
        menu.setSortOrder(sort);
        menu.setVisible(true);
        menu.setEnabled(true);
        return menu;
    }

    private static Menu button(String name, String permission, Long parentId, int sort) {
        return menu(name, "button", null, null, permission, parentId, sort);
    }
}
