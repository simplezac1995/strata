// SPDX-License-Identifier: Apache-2.0
import {
  createContext,
  useContext,
  useState,
  useEffect,
  useMemo,
  useCallback,
  type ReactNode,
} from "react";
import { useI18nStore } from "@/shared/i18n";
import { STRATA_PRIMARY } from "@/theme/muiTheme";

export type Lang = "zh" | "en";
export type Density = "default" | "compact";
export type SidebarTheme = "dark" | "light" | "carbon" | "forest" | "crimson" | "custom";
export type ColorMode = "light" | "dark" | "system";

const APPEARANCE_PREFS_KEY = "strata_appearance_prefs:v1";

interface AppearancePrefs {
  colorMode?: ColorMode;
  density?: Density;
  sidebarTheme?: SidebarTheme;
}

function readAppearancePrefs(): AppearancePrefs {
  try {
    const raw = localStorage.getItem(APPEARANCE_PREFS_KEY);
    if (!raw) return {};
    const parsed = JSON.parse(raw) as AppearancePrefs;
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

function writeAppearancePrefs(prefs: AppearancePrefs) {
  try {
    localStorage.setItem(
      APPEARANCE_PREFS_KEY,
      JSON.stringify({ ...readAppearancePrefs(), ...prefs }),
    );
  } catch {
    /* quota / private mode */
  }
}

function resolveDark(mode: ColorMode): boolean {
  if (mode === "dark") return true;
  if (mode === "light") return false;
  return window.matchMedia("(prefers-color-scheme: dark)").matches;
}

interface AppearanceCtx {
  dark: boolean;
  colorMode: ColorMode;
  setColorMode: (m: ColorMode) => void;
  toggleDark: () => void;
  lang: Lang;
  setLang: (l: Lang) => void;
  density: Density;
  setDensity: (d: Density) => void;
  sidebarTheme: SidebarTheme;
  setSidebarTheme: (t: SidebarTheme) => void;
  customSidebarBg: string;
  setCustomSidebarBg: (c: string) => void;
  customSidebarAccent: string;
  setCustomSidebarAccent: (c: string) => void;
}

const Ctx = createContext<AppearanceCtx>(null!);
export const useAppearance = () => useContext(Ctx);

export function AppearanceProvider({ children }: { children: ReactNode }) {
  const saved = readAppearancePrefs();
  const [colorMode, setColorModeState] = useState<ColorMode>(saved.colorMode ?? "light");
  const [dark, setDark] = useState(() => resolveDark(saved.colorMode ?? "light"));
  const i18nLang = useI18nStore((s) => s.lang);
  const setI18nLang = useI18nStore((s) => s.setLang);
  const [lang, setLang] = useState<Lang>(i18nLang);
  const [density, setDensityState] = useState<Density>(saved.density ?? "default");
  const [sidebarTheme, setSidebarThemeState] = useState<SidebarTheme>(saved.sidebarTheme ?? "dark");
  const [customSidebarBg, setCustomSidebarBg] = useState("#1C1D20");
  const [customSidebarAccent, setCustomSidebarAccent] = useState(STRATA_PRIMARY);

  const setColorMode = useCallback((mode: ColorMode) => {
    setColorModeState(mode);
    writeAppearancePrefs({ colorMode: mode });
  }, []);

  const toggleDark = useCallback(() => {
    setColorMode(dark ? "light" : "dark");
  }, [dark, setColorMode]);

  const setLangSync = useCallback(
    (l: Lang) => {
      setI18nLang(l);
      setLang(l);
    },
    [setI18nLang],
  );

  const setDensity = useCallback((d: Density) => {
    setDensityState(d);
    writeAppearancePrefs({ density: d });
  }, []);

  const setSidebarTheme = useCallback((t: SidebarTheme) => {
    setSidebarThemeState(t);
    writeAppearancePrefs({ sidebarTheme: t });
  }, []);

  useEffect(() => {
    if (colorMode === "system") {
      const mq = window.matchMedia("(prefers-color-scheme: dark)");
      const apply = () => setDark(mq.matches);
      apply();
      mq.addEventListener("change", apply);
      return () => mq.removeEventListener("change", apply);
    }
    setDark(colorMode === "dark");
  }, [colorMode]);

  /* Density reflected as body classes for CSS variables used by shell nav. */
  useEffect(() => {
    document.body.classList.remove("density-default", "density-compact");
    document.body.classList.add(`density-${density}`);
  }, [density]);

  const value = useMemo<AppearanceCtx>(
    () => ({
      dark,
      colorMode,
      setColorMode,
      toggleDark,
      lang,
      setLang: setLangSync,
      density,
      setDensity,
      sidebarTheme,
      setSidebarTheme,
      customSidebarBg,
      setCustomSidebarBg,
      customSidebarAccent,
      setCustomSidebarAccent,
    }),
    [
      dark,
      colorMode,
      setColorMode,
      toggleDark,
      lang,
      setLangSync,
      density,
      setDensity,
      sidebarTheme,
      setSidebarTheme,
      customSidebarBg,
      customSidebarAccent,
    ],
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}
