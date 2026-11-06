// SPDX-License-Identifier: Apache-2.0
import { describe, it, expect } from "vitest";
import { createMemoryRouter, RouterProvider, Outlet } from "react-router-dom";
import { render, screen } from "@testing-library/react";
import { createElement } from "react";

describe("nested system routes", () => {
  it("matches /system/site-settings under parent system group", async () => {
    const Site = () => createElement("div", null, "SITE_SETTINGS_PAGE");
    const I18n = () => createElement("div", null, "I18N_PAGE");
    const Shell = () => createElement(Outlet);

    const router = createMemoryRouter(
      [
        {
          path: "/",
          element: createElement(Shell),
          children: [
            {
              path: "system",
              children: [
                { path: "i18n", element: createElement(I18n) },
                { path: "site-settings", element: createElement(Site) },
              ],
            },
          ],
        },
      ],
      { initialEntries: ["/system/site-settings"] },
    );

    render(createElement(RouterProvider, { router }));
    expect(await screen.findByText("SITE_SETTINGS_PAGE")).toBeTruthy();
  });
});
