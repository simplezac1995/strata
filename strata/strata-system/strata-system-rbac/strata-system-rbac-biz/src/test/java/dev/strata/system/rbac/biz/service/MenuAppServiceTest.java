// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.MenuDto;
import dev.strata.system.api.request.MenuCreateRequest;
import dev.strata.system.api.request.MenuUpdateRequest;
import dev.strata.system.rbac.api.entity.Menu;
import dev.strata.system.rbac.biz.mapper.MenuMapper;
import dev.strata.system.rbac.biz.repository.MenuRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MenuAppServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private MenuMapper menuMapper;

    @InjectMocks
    private MenuAppService menuAppService;

    private Menu testMenu;
    private MenuDto testMenuDto;

    @BeforeEach
    void setUp() {
        testMenu = new Menu();
        testMenu.setId(1L);
        testMenu.setParentId(0L);
        testMenu.setName("系统管理");
        testMenu.setType("MENU");
        testMenu.setPath("/system");
        testMenu.setComponent("SystemLayout");
        testMenu.setPermission("system:view");
        testMenu.setSortOrder(1);
        testMenu.setVisible(true);
        testMenu.setEnabled(true);

        testMenuDto = new MenuDto(
                "1", null, "系统管理", "MENU", "/system", "SystemLayout", "system:view", 1, true, true, new ArrayList<>());
    }

    @Nested
    class CreateMenuTests {

        @Test
        void createMenu_Success() {
            MenuCreateRequest request = new MenuCreateRequest(
                    "0", "用户管理", "MENU", "/system/user", "UserPage", "system:user:view", 2, true, true);

            when(menuMapper.toEntity(request)).thenReturn(testMenu);
            when(menuRepository.save(any(Menu.class))).thenReturn(testMenu);
            when(menuMapper.toDto(any(Menu.class))).thenReturn(testMenuDto);

            MenuDto result = menuAppService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("系统管理");
            verify(menuRepository).save(any(Menu.class));
        }
    }

    @Nested
    class UpdateMenuTests {

        @Test
        void updateMenu_Success() {
            MenuUpdateRequest request = new MenuUpdateRequest(
                    "0", "更新后的菜单", "MENU", "/system/updated", "UpdatedPage", "system:updated:view", 3, true, true);

            when(menuRepository.findById(1L)).thenReturn(Optional.of(testMenu));
            when(menuRepository.save(any(Menu.class))).thenReturn(testMenu);
            when(menuMapper.toDto(any(Menu.class))).thenReturn(testMenuDto);

            MenuDto result = menuAppService.update(1L, request);

            assertThat(result).isNotNull();
            verify(menuMapper).update(any(MenuUpdateRequest.class), any(Menu.class));
            verify(menuRepository).save(any(Menu.class));
        }

        @Test
        void updateMenu_NotFound_ThrowsException() {
            MenuUpdateRequest request = new MenuUpdateRequest("0", "更新", "MENU", null, null, null, null, null, null);

            when(menuRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> menuAppService.update(999L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }
    }

    @Nested
    class DeleteMenuTests {

        @Test
        void deleteMenu_Success() {
            when(menuRepository.findById(1L)).thenReturn(Optional.of(testMenu));

            menuAppService.delete(1L);

            verify(menuRepository).delete(testMenu);
        }

        @Test
        void deleteMenu_NotFound_ThrowsException() {
            when(menuRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> menuAppService.delete(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }
    }

    @Nested
    class GetMenuTests {

        @Test
        void getMenu_Success() {
            when(menuRepository.findById(1L)).thenReturn(Optional.of(testMenu));
            when(menuMapper.toDto(any(Menu.class))).thenReturn(testMenuDto);

            MenuDto result = menuAppService.get(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("1");
            assertThat(result.name()).isEqualTo("系统管理");
        }

        @Test
        void getMenu_NotFound_ThrowsException() {
            when(menuRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> menuAppService.get(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }
    }

    @Nested
    class ListMenusTests {

        @Test
        void listMenus_Success() {
            Menu menu2 = new Menu();
            menu2.setId(2L);
            menu2.setName("用户管理");
            menu2.setType("MENU");
            menu2.setEnabled(true);

            MenuDto dto2 = new MenuDto(
                    "2",
                    "1",
                    "用户管理",
                    "MENU",
                    "/system/user",
                    "UserPage",
                    "system:user:view",
                    2,
                    true,
                    true,
                    new ArrayList<>());

            when(menuRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(testMenu, menu2));
            when(menuMapper.toDto(any(Menu.class))).thenReturn(testMenuDto, dto2);

            List<MenuDto> result = menuAppService.list();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("系统管理");
            assertThat(result.get(0).children()).hasSize(1);
            assertThat(result.get(0).children().get(0).name()).isEqualTo("用户管理");
        }
    }

    @Nested
    class ListChildrenTests {

        @Test
        void listChildren_Success() {
            Menu child1 = new Menu();
            child1.setId(2L);
            child1.setParentId(1L);
            child1.setName("用户管理");
            child1.setEnabled(true);

            Menu child2 = new Menu();
            child2.setId(3L);
            child2.setParentId(1L);
            child2.setName("角色管理");
            child2.setEnabled(true);

            when(menuRepository.findByParentIdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(child1, child2));
            when(menuMapper.toDto(any(Menu.class))).thenReturn(testMenuDto, testMenuDto);

            List<MenuDto> result = menuAppService.listChildren(1L);

            assertThat(result).hasSize(2);
            verify(menuRepository).findByParentIdOrderBySortOrderAscIdAsc(1L);
        }
    }

    @Nested
    class PageFlatTests {

        @Test
        void pageFlat_Success() {
            Page<Menu> menuPage = new PageImpl<>(List.of(testMenu));

            when(menuRepository.search(anyString(), any(Pageable.class))).thenReturn(menuPage);
            when(menuMapper.toDto(any(Menu.class))).thenReturn(testMenuDto);

            PageResult<MenuDto> result = menuAppService.pageFlat("系统", 0, 10);

            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }
    }
}
