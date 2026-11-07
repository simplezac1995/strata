// SPDX-License-Identifier: Apache-2.0
import { describe, it, expect, afterEach } from "vitest";
import { render, screen, fireEvent, cleanup } from "@testing-library/react";
import { AppProvider, useApp } from "@/context/AppContext";

function TabProbe() {
  const { tabs, openTab, closeOtherTabs, activeTab } = useApp();

  return (
    <div>
      <button
        type="button"
        onClick={() => openTab({ id: "users", label: "nav.users", path: "/system/users" })}
      >
        open-users
      </button>
      <button
        type="button"
        onClick={() => openTab({ id: "roles", label: "nav.roles", path: "/system/roles" })}
      >
        open-roles
      </button>
      <button type="button" onClick={() => closeOtherTabs("users")}>
        close-other
      </button>
      <span data-testid="tab-count">{tabs.length}</span>
      <span data-testid="active-tab">{activeTab}</span>
    </div>
  );
}

describe("tab management", () => {
  afterEach(() => {
    cleanup();
  });

  it("closeOtherTabs keeps only the selected tab", () => {
    render(
      <AppProvider>
        <TabProbe />
      </AppProvider>,
    );

    fireEvent.click(screen.getByRole("button", { name: "open-users" }));
    fireEvent.click(screen.getByRole("button", { name: "open-roles" }));
    expect(screen.getByTestId("tab-count").textContent).toBe("3");

    fireEvent.click(screen.getByRole("button", { name: "close-other" }));
    expect(screen.getByTestId("tab-count").textContent).toBe("1");
    expect(screen.getByTestId("active-tab").textContent).toBe("users");
  });
});
