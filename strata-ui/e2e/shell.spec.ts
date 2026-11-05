import { expect, test } from "@playwright/test";
import { loginAsAdmin } from "./support/auth";

async function brandContrast(page: import("@playwright/test").Page) {
  return page.getByTestId("sidebar-brand-title").evaluate((title) => {
    const parseRgb = (color: string) => color.match(/\d+/g)?.slice(0, 3).map(Number) ?? [];
    const luminance = ([red, green, blue]: number[]) =>
      [red, green, blue].reduce((sum, channel, index) => {
        const normalized = channel / 255;
        const linear =
          normalized <= 0.03928 ? normalized / 12.92 : ((normalized + 0.055) / 1.055) ** 2.4;
        return sum + linear * [0.2126, 0.7152, 0.0722][index];
      }, 0);
    const foreground = luminance(parseRgb(getComputedStyle(title).color));
    const sidebar = title.closest("aside");
    const background = luminance(parseRgb(getComputedStyle(sidebar!).backgroundColor));
    return (Math.max(foreground, background) + 0.05) / (Math.min(foreground, background) + 0.05);
  });
}

test("sidebar brand keeps accessible contrast across content modes", async ({ page }) => {
  await loginAsAdmin(page);
  await page.goto("/settings/appearance");

  for (const sidebarTheme of ["dark", "light", "carbon", "forest", "crimson", "custom"]) {
    await page.getByTestId(`sidebar-theme-${sidebarTheme}`).click();
    for (const mode of ["light", "dark"]) {
      await page.getByTestId(`color-mode-${mode}`).click();
      await expect(page.getByTestId("sidebar-brand-title")).toBeVisible();
      await expect.poll(() => brandContrast(page), { timeout: 1_000 }).toBeGreaterThanOrEqual(4.5);
    }
  }
});
