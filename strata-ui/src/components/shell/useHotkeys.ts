// SPDX-License-Identifier: Apache-2.0
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useApp, type AppTab } from "@/context/AppContext";

/**
 * Custom event pages can listen to trigger their own "save" action.
 *
 * Ctrl/Cmd+S is a global shell shortcut: the shell dispatches this event and
 * never blocks the browser save dialog unless a page opted in. Pages that own
 * a form listen via `window.addEventListener(STRATA_SAVE_EVENT, ...)` and
 * call their own submit handler. This keeps the shortcut wiring centralized
 * without forcing every page to register its own keybinding.
 */
export const STRATA_SAVE_EVENT = "strata:save";

const DASHBOARD_TAB_ID = "dashboard";

/**
 * Centralized global keyboard shortcuts for the admin shell.
 *
 * Registered shortcuts (all Cmd/Ctrl on macOS, Ctrl elsewhere):
 * - `K`  — open global search (existing behavior, kept here)
 * - `B`  — toggle the sidebar (existing behavior, kept here)
 * - `W`  — close the current in-app tab (prevents the browser tab from being
 *   closed when the intercept is allowed; closes the multi-tab instead)
 * - `]` / `[` — move to the next / previous open tab
 * - `1..9` — jump directly to the Nth open tab
 * - `S`  — dispatch {@link STRATA_SAVE_EVENT} so the active page can save
 *
 * Mount once in AppShell. Does not intercept keys while the user is typing in
 * a text field (except the always-global `K` / `S` save events).
 */
export function useHotkeys() {
  const { tabs, activeTab, closeTab, openTab, setSearchOpen, toggleSidebar } = useApp();
  const navigate = useNavigate();

  useEffect(() => {
    const activate = (tab: AppTab) => {
      openTab(tab);
      navigate(tab.path);
    };

    const closeCurrentTab = () => {
      if (!activeTab || activeTab === DASHBOARD_TAB_ID) return;
      const remaining = tabs.filter((t) => t.id !== activeTab);
      closeTab(activeTab);
      const next =
        remaining.length > 0
          ? remaining[remaining.length - 1]
          : { id: DASHBOARD_TAB_ID, label: "nav.dashboard", path: "/dashboard" };
      navigate(next.path);
    };

    const stepTab = (delta: 1 | -1) => {
      if (tabs.length === 0) return;
      const idx = tabs.findIndex((t) => t.id === activeTab);
      const from = idx >= 0 ? idx : 0;
      const next = tabs[(from + delta + tabs.length) % tabs.length];
      if (next) activate(next);
    };

    const jumpTab = (n: number) => {
      const target = tabs[n];
      if (target) activate(target);
    };

    const handler = (e: KeyboardEvent) => {
      const mod = e.metaKey || e.ctrlKey;
      if (!mod) return;

      const key = e.key.toLowerCase();
      const target = e.target as HTMLElement | null;
      const typing =
        !!target &&
        (target.tagName === "INPUT" || target.tagName === "TEXTAREA" || target.isContentEditable);

      // Cmd/Ctrl+K — always available (even while typing) so search stays global.
      if (key === "k") {
        e.preventDefault();
        setSearchOpen(true);
        return;
      }

      // While typing in a field, only the global search and save shortcuts are
      // honored; tab navigation must not hijack text input.
      if (typing && key !== "s") return;

      switch (key) {
        case "b":
          e.preventDefault();
          toggleSidebar();
          break;
        case "w":
          // Best-effort: some browsers reserve Ctrl+W and won't let us prevent
          // closing the window. We still close the in-app tab when allowed.
          e.preventDefault();
          closeCurrentTab();
          break;
        case "s":
          e.preventDefault();
          window.dispatchEvent(new CustomEvent(STRATA_SAVE_EVENT));
          break;
        case "]":
          e.preventDefault();
          stepTab(1);
          break;
        case "[":
          e.preventDefault();
          stepTab(-1);
          break;
        default:
          if (key >= "1" && key <= "9") {
            e.preventDefault();
            jumpTab(Number(key) - 1);
          }
      }
    };

    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [tabs, activeTab, closeTab, openTab, setSearchOpen, toggleSidebar, navigate]);
}
