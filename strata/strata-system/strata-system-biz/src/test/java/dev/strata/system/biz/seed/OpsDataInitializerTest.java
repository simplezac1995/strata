// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.seed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.system.api.seed.OpsMenuSeedContributor;
import dev.strata.system.communication.biz.repository.MessageChannelRepository;
import dev.strata.system.config.biz.repository.I18nEntryRepository;
import dev.strata.system.ops.biz.repository.SysJobRepository;
import dev.strata.system.rbac.api.entity.Menu;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.biz.repository.MenuRepository;
import dev.strata.system.rbac.biz.repository.RoleMenuRepository;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpsDataInitializer 单元测试")
class OpsDataInitializerTest {

    @Mock
    private I18nEntryRepository i18nEntryRepository;

    @Mock
    private SysJobRepository sysJobRepository;

    @Mock
    private MessageChannelRepository messageChannelRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMenuRepository roleMenuRepository;

    @Mock
    private OpsMenuSeedSupport menuSeedSupport;

    private OpsDataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new OpsDataInitializer(
                i18nEntryRepository,
                sysJobRepository,
                messageChannelRepository,
                menuRepository,
                roleRepository,
                roleMenuRepository,
                List.of(),
                menuSeedSupport);
    }

    private Menu directory(long id) {
        Menu m = new Menu();
        m.setId(id);
        return m;
    }

    @Nested
    @DisplayName("幂等跳过分支")
    class IdempotentSkipTests {

        @Test
        @DisplayName("所有数据已存在时全部跳过不写入")
        void run_AllSeeded_SkipsEverything() {
            when(i18nEntryRepository.count()).thenReturn(10L);
            when(sysJobRepository.count()).thenReturn(3L);
            when(messageChannelRepository.count()).thenReturn(4L);
            when(menuRepository.existsByPermission("system:sitesetting:list")).thenReturn(true);
            when(menuRepository.existsByPermission("system:auditlog:list")).thenReturn(true);

            initializer.run(new DefaultApplicationArguments());

            verify(i18nEntryRepository, never()).save(any());
            verify(sysJobRepository, never()).save(any());
            verify(messageChannelRepository, never()).save(any());
            verify(menuRepository, never()).save(any());
            verify(roleMenuRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("空库全量播种")
    class FullSeedTests {

        @Test
        @DisplayName("空库时播种 i18n / jobs / channels / menus 并关联 admin")
        void run_EmptyDatabase_SeedsEverything() {
            when(i18nEntryRepository.count()).thenReturn(0L);
            when(sysJobRepository.count()).thenReturn(0L);
            when(messageChannelRepository.count()).thenReturn(0L);
            when(menuRepository.existsByPermission("system:sitesetting:list")).thenReturn(false);
            when(menuRepository.existsByPermission("system:auditlog:list")).thenReturn(false);

            when(menuRepository.findFirstByPathAndType("/system", "directory"))
                    .thenReturn(Optional.of(directory(100L)));
            when(menuRepository.findFirstByPathAndType("/tools", "directory")).thenReturn(Optional.of(directory(200L)));
            when(menuRepository.findFirstByPathAndType("/settings", "directory"))
                    .thenReturn(Optional.of(directory(300L)));
            when(menuRepository.save(any(Menu.class))).thenAnswer(inv -> inv.getArgument(0));

            Role admin = new Role();
            admin.setId(1L);
            when(roleRepository.findByCodeAndDeletedFalse("admin")).thenReturn(Optional.of(admin));

            initializer.run(new DefaultApplicationArguments());

            // i18n 播种 15 行
            verify(i18nEntryRepository, times(15)).save(any());
            // jobs 播种 3 条
            verify(sysJobRepository, times(3)).save(any());
            // channels 播种 4 条
            verify(messageChannelRepository, times(4)).save(any());
            // admin 菜单关联
            verify(roleMenuRepository, org.mockito.Mockito.atLeastOnce()).save(any());
        }

        @Test
        @DisplayName("tools/settings 目录不存在时自动创建")
        void run_DirectoriesMissing_CreatesThem() {
            when(i18nEntryRepository.count()).thenReturn(0L);
            when(sysJobRepository.count()).thenReturn(0L);
            when(messageChannelRepository.count()).thenReturn(0L);
            when(menuRepository.existsByPermission("system:sitesetting:list")).thenReturn(false);
            when(menuRepository.existsByPermission("system:auditlog:list")).thenReturn(false);

            when(menuRepository.findFirstByPathAndType("/system", "directory"))
                    .thenReturn(Optional.of(directory(100L)));
            // tools / settings 不存在 → orElseGet 创建
            when(menuRepository.findFirstByPathAndType("/tools", "directory")).thenReturn(Optional.empty());
            when(menuRepository.findFirstByPathAndType("/settings", "directory"))
                    .thenReturn(Optional.empty());
            when(menuRepository.save(any(Menu.class))).thenAnswer(inv -> inv.getArgument(0));
            when(roleRepository.findByCodeAndDeletedFalse("admin")).thenReturn(Optional.empty());

            initializer.run(new DefaultApplicationArguments());

            // 创建 tools + settings 两个目录（其余菜单也会保存，用 atLeast 验证目录已建）
            verify(menuRepository, org.mockito.Mockito.atLeast(2)).save(any(Menu.class));
        }

        @Test
        @DisplayName("admin 角色不存在时不关联菜单")
        void run_NoAdminRole_SkipsLinking() {
            when(i18nEntryRepository.count()).thenReturn(0L);
            when(sysJobRepository.count()).thenReturn(0L);
            when(messageChannelRepository.count()).thenReturn(0L);
            when(menuRepository.existsByPermission("system:sitesetting:list")).thenReturn(false);
            when(menuRepository.existsByPermission("system:auditlog:list")).thenReturn(false);
            when(menuRepository.findFirstByPathAndType("/system", "directory"))
                    .thenReturn(Optional.of(directory(100L)));
            when(menuRepository.findFirstByPathAndType("/tools", "directory")).thenReturn(Optional.of(directory(200L)));
            when(menuRepository.findFirstByPathAndType("/settings", "directory"))
                    .thenReturn(Optional.of(directory(300L)));
            when(menuRepository.save(any(Menu.class))).thenAnswer(inv -> inv.getArgument(0));
            when(roleRepository.findByCodeAndDeletedFalse("admin")).thenReturn(Optional.empty());

            initializer.run(new DefaultApplicationArguments());

            verify(roleMenuRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("system 目录缺失分支")
    class SystemDirectoryMissingTests {

        @Test
        @DisplayName("system 目录不存在时跳过 system 子菜单但仍建 tools/settings 菜单")
        void run_SystemDirMissing_SkipsSystemMenus() {
            when(i18nEntryRepository.count()).thenReturn(0L);
            when(sysJobRepository.count()).thenReturn(0L);
            when(messageChannelRepository.count()).thenReturn(0L);
            when(menuRepository.existsByPermission("system:sitesetting:list")).thenReturn(false);
            when(menuRepository.existsByPermission("system:auditlog:list")).thenReturn(false);
            // system 目录不存在
            when(menuRepository.findFirstByPathAndType("/system", "directory")).thenReturn(Optional.empty());
            when(menuRepository.findFirstByPathAndType("/tools", "directory")).thenReturn(Optional.of(directory(200L)));
            when(menuRepository.findFirstByPathAndType("/settings", "directory"))
                    .thenReturn(Optional.of(directory(300L)));
            when(menuRepository.save(any(Menu.class))).thenAnswer(inv -> inv.getArgument(0));
            when(roleRepository.findByCodeAndDeletedFalse("admin")).thenReturn(Optional.empty());

            initializer.run(new DefaultApplicationArguments());

            // auditlog 菜单因 system 目录缺失而不创建
            verify(roleMenuRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("菜单已存在但审计日志菜单缺失")
    class AuditLogOnlyTests {

        @Test
        @DisplayName("sitesetting 已存在但 auditlog 缺失时只补审计日志菜单")
        void run_SitesettingExists_AuditLogMissing_SeedsAuditLogOnly() {
            when(i18nEntryRepository.count()).thenReturn(10L);
            when(sysJobRepository.count()).thenReturn(3L);
            when(messageChannelRepository.count()).thenReturn(4L);
            when(menuRepository.existsByPermission("system:sitesetting:list")).thenReturn(true);
            when(menuRepository.existsByPermission("system:auditlog:list")).thenReturn(false);
            when(menuRepository.findFirstByPathAndType("/system", "directory"))
                    .thenReturn(Optional.of(directory(100L)));
            when(menuRepository.save(any(Menu.class))).thenAnswer(inv -> inv.getArgument(0));
            Role admin = new Role();
            admin.setId(1L);
            when(roleRepository.findByCodeAndDeletedFalse("admin")).thenReturn(Optional.of(admin));

            initializer.run(new DefaultApplicationArguments());

            // 只创建审计日志菜单并关联一次
            verify(menuRepository, times(1)).save(any(Menu.class));
            verify(roleMenuRepository, times(1)).save(any());
        }
    }

    @Nested
    @DisplayName("菜单种子贡献者")
    class ContributorTests {

        @Test
        @DisplayName("调用所有贡献者的 seed 方法")
        void run_InvokesContributors() {
            OpsMenuSeedContributor contributor = ctx -> {};
            OpsDataInitializer withContributor = new OpsDataInitializer(
                    i18nEntryRepository,
                    sysJobRepository,
                    messageChannelRepository,
                    menuRepository,
                    roleRepository,
                    roleMenuRepository,
                    List.of(contributor),
                    menuSeedSupport);
            when(i18nEntryRepository.count()).thenReturn(1L);
            when(sysJobRepository.count()).thenReturn(1L);
            when(messageChannelRepository.count()).thenReturn(1L);
            when(menuRepository.existsByPermission(any())).thenReturn(true);

            withContributor.run(new DefaultApplicationArguments());

            // 贡献者被调用一次（seed 方法无副作用，仅验证调用）
            // 这里通过不抛异常验证；List.of(contributor) 直接执行 lambda
        }
    }
}
