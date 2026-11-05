import { expect, test } from "@playwright/test";
import { loginAsAdmin } from "./support/auth";

test("users page refreshes the session after one unauthorized response", async ({ page }) => {
  await loginAsAdmin(page);

  let rejected = false;
  await page.route("**/api/v1/system/users/page?*", async (route) => {
    if (!rejected) {
      rejected = true;
      await route.fulfill({
        status: 401,
        contentType: "application/problem+json",
        body: JSON.stringify({ status: 401, title: "Unauthorized" }),
      });
      return;
    }
    await route.continue();
  });

  const refreshResponse = page.waitForResponse(
    (response) =>
      new URL(response.url()).pathname === "/api/v1/system/auth/refresh" &&
      response.status() === 200,
  );
  const retriedPageResponse = page.waitForResponse(
    (response) =>
      new URL(response.url()).pathname === "/api/v1/system/users/page" && response.status() === 200,
  );

  await page.goto("/system/users");

  await refreshResponse;
  const response = await retriedPageResponse;
  const parameters = new URL(response.url()).searchParams;
  expect(parameters.get("page")).toBe("0");
  expect(parameters.get("size")).toBe("10");
  await expect(page).toHaveURL(/\/system\/users$/);
  await expect(page.getByRole("heading", { name: /users|用户/i })).toBeVisible();
});
