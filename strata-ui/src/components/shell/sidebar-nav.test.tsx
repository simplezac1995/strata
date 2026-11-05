// SPDX-License-Identifier: Apache-2.0
import { Box } from "@mui/material";
import { describe, it, expect } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter, Route, Routes, Outlet } from "react-router-dom";
import { AppProvider } from "@/context/AppContext";
import { Sidebar } from "@/components/shell/Sidebar";

function Layout() {
  return (
    <Box sx={{ display: "flex", height: "100vh" }}>
      <Sidebar />
      <Outlet />
    </Box>
  );
}

function Shell({ initial }: { initial: string }) {
  return (
    <AppProvider>
      <MemoryRouter initialEntries={[initial]}>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/dashboard" element={<div>DASHBOARD_PAGE</div>} />
            <Route path="/notifications" element={<div>NOTIFICATIONS_PAGE</div>} />
            <Route path="/system/i18n" element={<div>I18N_PAGE</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </AppProvider>
  );
}

describe("Sidebar navigation", () => {
  it("navigates to notifications from dashboard", async () => {
    render(<Shell initial="/dashboard" />);
    expect(screen.getByText("DASHBOARD_PAGE")).toBeTruthy();

    fireEvent.click(screen.getByRole("link", { name: "通知中心" }));
    expect(await screen.findByText("NOTIFICATIONS_PAGE")).toBeTruthy();
  });

  it("navigates to i18n under system group", async () => {
    render(<Shell initial="/dashboard" />);

    fireEvent.click(screen.getAllByRole("button", { name: "系统管理" })[0]!);
    fireEvent.click(screen.getByRole("button", { name: "基础配置" }));
    fireEvent.click(screen.getByRole("link", { name: "国际化" }));
    expect(await screen.findByText("I18N_PAGE")).toBeTruthy();
  });
});
