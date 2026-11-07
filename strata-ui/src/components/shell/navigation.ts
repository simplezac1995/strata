// SPDX-License-Identifier: Apache-2.0
import { SWAGGER_UI_URL } from "@/shared/config";
import type { MenuDto } from "@/shared/types";
import type { NavEntry, NavItem, SearchNavItem } from "@/components/shell/navTypes";
import { isSeparator } from "@/components/shell/navTypes";
import { NAV_OVERVIEW } from "./nav/overview";
import { NAV_SYSTEM } from "./nav/system";
import { NAV_WORKSPACE } from "./nav/workspace";
import { NAV_BIGMODEL } from "./nav/bigmodel";
import { NAV_ACCOUNT } from "./nav/account";
export type { IconType, NavItem, NavEntry, SearchNavItem } from "@/components/shell/navTypes";
export { isSeparator } from "@/components/shell/navTypes";

export const NAV: NavEntry[] = [
  ...NAV_OVERVIEW,
  ...NAV_SYSTEM,
  ...NAV_WORKSPACE,
  ...NAV_BIGMODEL,
  ...NAV_ACCOUNT,
];

/**
 * Derives a route's display label from NAV (single source of truth for
 * path →label). Returns undefined when the path has no sidebar entry.
 */
export function routeLabel(path: string): string | undefined {
  const walk = (items: NavItem[]): string | undefined => {
    for (const item of items) {
      if (item.path === path) return item.label;
      if (item.children) {
        const found = walk(item.children);
        if (found !== undefined) return found;
      }
    }
    return undefined;
  };
  return walk(NAV.filter((e): e is NavItem => !isSeparator(e)));
}

/** Opens external links in a new tab; returns in-app path when applicable. */
export function openNavItem(
  item: Pick<NavItem, "path" | "external" | "externalHref">,
): string | null {
  if (item.external) {
    window.open(item.externalHref ?? SWAGGER_UI_URL, "_blank", "noopener,noreferrer");
    return null;
  }
  return item.path ?? null;
}

/** Flatten an arbitrary nav entry tree into searchable routes (labels are i18n keys). */
export function flattenNavEntries(entries: NavEntry[]): SearchNavItem[] {
  const out: SearchNavItem[] = [];

  const walk = (items: NavItem[], section: string) => {
    for (const item of items) {
      const sec = item.children?.length ? item.label : section;
      if (item.path) {
        out.push({ label: item.label, path: item.path, icon: item.icon, section: sec });
      } else if (item.external) {
        const href = item.externalHref ?? SWAGGER_UI_URL;
        out.push({
          label: item.label,
          path: href,
          icon: item.icon,
          section,
          external: true,
          externalHref: href,
        });
      }
      if (item.children) walk(item.children, item.label);
    }
  };

  let section = "nav.overview";
  for (const entry of entries) {
    if (isSeparator(entry)) {
      section = entry.label;
      continue;
    }
    if (entry.path) {
      out.push({ label: entry.label, path: entry.path, icon: entry.icon, section });
    }
    if (entry.children) walk(entry.children, entry.label);
  }
  return out;
}

/** Flat searchable routes derived from the static NAV (labels are short i18n keys). */
export function flattenNavForSearch(): SearchNavItem[] {
  return flattenNavEntries(NAV);
}

/**
 * Visible-label set collected from the current user's backend menu tree (sys_menu, already filtered to the
 * roles held by the user). Buttons (type=button) are permissions, not nav entries, and are excluded.
 */
function collectVisibleLabels(menus: MenuDto[]): Set<string> {
  const visible = new Set<string>();
  const walk = (nodes: MenuDto[]) => {
    for (const m of nodes ?? []) {
      if (m.type === "button" || !m.enabled || !m.visible) continue;
      visible.add(m.name);
      if (m.children) walk(m.children);
    }
  };
  walk(menus);
  return visible;
}

/**
 * Drives the sidebar from sys_menu WITHOUT disturbing the curated layout: the static NAV keeps its
 * section separators, group nesting, icons and order; the backend menu tree only decides WHICH entries
 * the current user may see.
 *
 * An entry is kept when either (a) it has no permission gate (public pages like dashboard / notifications,
 * which sys_menu does not own), or (b) its i18n label (`sys_menu.name` == NAV `label`) is present in the
 * user's menu tree. Groups with no surviving visible child are dropped, and separators are kept only when
 * a visible group follows them.
 */
export function deriveNavFromMenus(menus: MenuDto[]): NavEntry[] {
  const visible = collectVisibleLabels(menus);

  const filterItems = (items: NavItem[]): NavItem[] => {
    const out: NavItem[] = [];
    for (const it of items) {
      if (it.children) {
        const children = filterItems(it.children);
        if (children.length > 0) {
          out.push({ ...it, children });
        } else if (!it.perm && visible.has(it.label)) {
          out.push({ ...it, children: undefined });
        }
        continue;
      }
      if (!it.perm || visible.has(it.label)) out.push(it);
    }
    return out;
  };

  const out: NavEntry[] = [];
  let pendingSep: string | null = null;
  for (const e of NAV) {
    if (isSeparator(e)) {
      pendingSep = e.label;
      continue;
    }
    const children = e.children ? filterItems(e.children) : undefined;
    const shows = children && children.length > 0 ? true : !e.perm || visible.has(e.label);
    if (!shows) continue;
    if (pendingSep && out.length > 0) out.push({ type: "separator", label: pendingSep });
    pendingSep = null;
    out.push(children && children.length > 0 ? { ...e, children } : e);
  }
  return out;
}
