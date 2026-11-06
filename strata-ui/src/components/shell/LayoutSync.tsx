// SPDX-License-Identifier: Apache-2.0
import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { useApp } from "@/context/AppContext";

/** Keeps mix/split active section in sync with the current route. */
export function LayoutSync() {
  const { layoutMode, setActiveNavSection } = useApp();
  const location = useLocation();

  useEffect(() => {
    if (layoutMode !== "mix" && layoutMode !== "split") return;
    const seg = location.pathname.split("/").filter(Boolean)[0];
    if (seg) setActiveNavSection(seg);
  }, [layoutMode, location.pathname, setActiveNavSection]);

  return null;
}
