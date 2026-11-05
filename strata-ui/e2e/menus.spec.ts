import { test, expect } from "@playwright/test";
import { loginAsAdmin } from "./support/auth";

test("menus list loads", async ({ page }) => {
  await loginAsAdmin(page);
  await page.goto("/system/menus");
  await expect(page.getByRole("heading", { name: /menus|菜单/i })).toBeVisible();
});
