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
];
