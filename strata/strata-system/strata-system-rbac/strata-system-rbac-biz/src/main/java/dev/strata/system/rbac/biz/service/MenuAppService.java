// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.security.audit.AuditLog;
import dev.strata.system.api.dto.MenuDto;
import dev.strata.system.api.request.MenuCreateRequest;
import dev.strata.system.api.request.MenuUpdateRequest;
import dev.strata.system.rbac.api.entity.Menu;
import dev.strata.system.rbac.biz.mapper.MenuMapper;
import dev.strata.system.rbac.biz.repository.MenuRepository;
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
public class MenuAppService {

    private static final Logger log = LoggerFactory.getLogger(MenuAppService.class);

    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;

    public MenuAppService(MenuRepository menuRepository, MenuMapper menuMapper) {
        this.menuRepository = menuRepository;
        this.menuMapper = menuMapper;
    }

    @Transactional(readOnly = true)
    public List<MenuDto> list() {
        return buildTree(menuRepository.findAllByOrderBySortOrderAscIdAsc());
    }

    @Transactional(readOnly = true)
    public List<MenuDto> listChildren(Long parentId) {
        List<Menu> menus = parentId == null
                ? menuRepository.findByParentIdIsNullOrderBySortOrderAscIdAsc()
                : menuRepository.findByParentIdOrderBySortOrderAscIdAsc(parentId);
        return menus.stream().map(menuMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PageResult<MenuDto> pageFlat(String keyword, int page, int size) {
        Page<Menu> result = menuRepository.search(
                keyword, PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "sortOrder", "id")));
        return PageResult.from(result.map(menuMapper::toDto));
    }

    @Transactional(readOnly = true)
    public MenuDto get(Long id) {
        return menuMapper.toDto(require(id));
    }

    @OperationLog(module = "system:menu", action = "create")
    @Transactional
    @AuditLog(action = "MENU_CREATE", description = "Create new menu", category = "MENU_MANAGEMENT")
    public MenuDto create(MenuCreateRequest request) {
        Menu menu = menuMapper.toEntity(request);
        menuRepository.save(menu);
        return menuMapper.toDto(menu);
    }

    @OperationLog(module = "system:menu", action = "update")
    @Transactional
    @AuditLog(action = "MENU_UPDATE", description = "Update menu", category = "MENU_MANAGEMENT")
    public MenuDto update(Long id, MenuUpdateRequest request) {
        Menu menu = require(id);
        menuMapper.update(request, menu);
        menuRepository.save(menu);
        return menuMapper.toDto(menu);
    }

    @OperationLog(module = "system:menu", action = "delete")
    @Transactional
    @AuditLog(action = "MENU_DELETE", description = "Delete menu", sensitive = true, category = "MENU_MANAGEMENT")
    public void delete(Long id) {
        menuRepository.delete(require(id));
    }

    /** Builds the menu tree for role menus (auth) and the admin menu page. */
    public List<MenuDto> buildTree(List<Menu> menus) {
        Map<Long, MenuDto> map = menus.stream().collect(Collectors.toMap(Menu::getId, menuMapper::toDto));
        List<MenuDto> roots = new ArrayList<>();
        for (MenuDto dto : map.values()) {
            if (dto.parentId() == null) {
                roots.add(dto);
            } else {
                MenuDto parent = map.get(Long.valueOf(dto.parentId()));
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

    private static void sortTree(List<MenuDto> nodes) {
        nodes.sort(Comparator.comparingInt(MenuDto::sortOrder).thenComparing(MenuDto::id));
        for (MenuDto node : nodes) {
            sortTree(node.children());
        }
    }

    private Menu require(Long id) {
        return menuRepository.findById(id).orElseThrow(() -> new NotFoundException("Menu not found"));
    }
}
