// SPDX-License-Identifier: Apache-2.0
// State and handlers of Menus, split out of Menus.tsx.

import { useCallback, useEffect, useState } from "react";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { menuApi, type MenuPayload } from "@/shared/api/system";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import { toast } from "@/shared/pageKit/Toast";
import type { MenuDto } from "@/shared/types";
import { updateTreeChildren, flatten, EMPTY_FORM } from "./part-1";

export function useMenus() {
  const { t } = useI18n();

  const canCreate = useAuthStore((s) => s.hasAuthority("system:menu:create"));

  const canUpdate = useAuthStore((s) => s.hasAuthority("system:menu:update"));

  const canDelete = useAuthStore((s) => s.hasAuthority("system:menu:delete"));

  const [menus, setMenus] = useState<MenuDto[]>([]);

  const [parentTree, setParentTree] = useState<MenuDto[]>([]);

  const [loading, setLoading] = useState(true);

  const reload = useCallback(async () => {
    setLoading(true);
    try {
      const [roots, full] = await Promise.all([menuApi.children(), menuApi.list()]);
      setMenus(roots);
      setParentTree(full);
    } catch {
      toast.error("toast.loadFailed");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  const expandNode = useCallback(async (parentId: string) => {
    try {
      const children = await menuApi.children(parentId);
      setMenus((prev) => updateTreeChildren(prev, parentId, children));
    } catch {
      toast.error("toast.loadFailed");
    }
  }, []);

  const [keyword, setKeyword] = useState("");

  const [appliedKeyword, setAppliedKeyword] = useState("");

  const [viewMode, setViewMode] = useState<"tree" | "flat">("tree");

  const {
    rows: flatMenus,
    total: flatTotal,
    page: flatPage,
    setPage: setFlatPage,
    pageSize: flatPageSize,
    setPageSize: setFlatPageSize,
    loading: flatLoading,
    reload: reloadFlat,
  } = usePagedQuery<MenuDto>(
    ["api", "system", "menus", "flat", { appliedKeyword }],
    (pageIndex, size) => menuApi.pageFlat(appliedKeyword || undefined, pageIndex, size),
  );

  function refreshAll() {
    void reload();
    if (viewMode === "flat") void reloadFlat();
  }

  const crud = useResourceCrud<MenuDto, MenuPayload, MenuPayload, typeof EMPTY_FORM>({
    create: (payload) => menuApi.create(payload),
    update: (id, payload) => menuApi.update(id, payload),
    remove: (id) => menuApi.remove(id),
    reload: refreshAll,
    initForm: () => ({ ...EMPTY_FORM }),
    toEditForm: (m) => ({
      parentId: m.parentId ?? "",
      name: m.name,
      type: m.type,
      path: m.path ?? "",
      component: m.component ?? "",
      permission: m.permission ?? "",
      sortOrder: m.sortOrder,
      visible: m.visible,
      enabled: m.enabled,
    }),
    toCreatePayload: (f) => ({
      parentId: f.parentId || null,
      name: f.name,
      type: f.type,
      path: f.path || null,
      component: f.component || null,
      permission: f.permission || null,
      sortOrder: f.sortOrder,
      visible: f.visible,
      enabled: f.enabled,
    }),
    toUpdatePayload: (f) => ({
      parentId: f.parentId || null,
      name: f.name,
      type: f.type,
      path: f.path || null,
      component: f.component || null,
      permission: f.permission || null,
      sortOrder: f.sortOrder,
      visible: f.visible,
      enabled: f.enabled,
    }),
  });

  function openCreate(parentId: string | null = null) {
    crud.openCreate();
    crud.setForm((f) => ({ ...f, parentId: parentId ?? "" }));
  }

  const parentOptions = [
    { value: "", label: t("system.menus.topLevel") },
    ...flatten(parentTree, t),
  ];

  const headers = [
    t("common.name"),
    t("common.type"),
    t("common.path"),
    t("system.menus.permissionCode"),
    t("common.sort"),
    t("common.enabled"),
    t("common.actions"),
  ];

  return {
    t,
    canCreate,
    canUpdate,
    canDelete,
    menus,
    setMenus,
    parentTree,
    setParentTree,
    loading,
    setLoading,
    reload,
    expandNode,
    keyword,
    setKeyword,
    appliedKeyword,
    setAppliedKeyword,
    viewMode,
    setViewMode,
    flatMenus,
    flatTotal,
    flatPage,
    setFlatPage,
    flatPageSize,
    setFlatPageSize,
    flatLoading,
    reloadFlat,
    refreshAll,
    crud,
    openCreate,
    parentOptions,
    headers,
  };
}
