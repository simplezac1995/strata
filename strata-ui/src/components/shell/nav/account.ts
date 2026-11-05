// SPDX-License-Identifier: Apache-2.0
// Sidebar navigation entries for the "nav.account" group, in display order.

import type { NavEntry } from "@/components/shell/navTypes";
import {
  Wrench,
  UserCircle,
  Lock,
  Monitor,
  Languages,
  Key,
  Users2,
  Star,
  Receipt,
  Pen,
  Bell,
} from "@/components/shell/navIcons";

export const NAV_ACCOUNT: NavEntry[] = [
  { type: "separator", label: "nav.account" },
  {
    id: "settings",
    label: "nav.settings",
    icon: UserCircle,
    children: [
      { id: "profile", label: "nav.profile", icon: UserCircle, path: "/settings/profile" },
      { id: "security", label: "nav.security", icon: Lock, path: "/settings/security" },
      { id: "appearance", label: "nav.appearance", icon: Monitor, path: "/settings/appearance" },
      { id: "language", label: "nav.language", icon: Languages, path: "/settings/language" },
      { id: "api-keys", label: "nav.api.keys", icon: Key, path: "/settings/api-keys" },
      { id: "team", label: "nav.team.members", icon: Users2, path: "/settings/team" },
      {
        id: "notif-settings",
        label: "nav.notifications",
        icon: Bell,
        path: "/settings/notifications",
      },
    ],
  },
  {
    id: "pro",
    label: "nav.pro",
    icon: Star,
    children: [
      { id: "workflow", label: "nav.workflow", icon: Wrench, path: "/pro/workflow", badge: "pro" },
      {
        id: "billing",
        label: "nav.tenants.billing",
        icon: Receipt,
        path: "/pro/billing",
        badge: "pro",
      },
      {
        id: "blog",
        label: "nav.content.blog",
        icon: Pen,
        path: "/pro/blog",
        badge: "pro",
        perm: "pro:blog:list",
      },
    ],
  },
];
