// SPDX-License-Identifier: Apache-2.0
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { RowActions, type RowAction } from "./RowActions";

const useMediaQueryMock = vi.fn();
vi.mock("@mui/material", async (importActual) => {
  const actual = await importActual<typeof import("@mui/material")>();
  return {
    ...actual,
    useMediaQuery: () => useMediaQueryMock(),
  };
});
vi.mock("@/shared/i18n", () => ({
  useI18n: () => ({ t: (k: string) => k }),
}));

describe("RowActions", () => {
  const actions: RowAction[] = [
    { key: "edit", label: "Edit", onClick: vi.fn() },
    { key: "delete", label: "Delete", onClick: vi.fn(), danger: true },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("renders nothing when actions are empty", () => {
    useMediaQueryMock.mockReturnValue(false);
    const { container } = render(<RowActions actions={[]} />);
    expect(container.querySelector("button")).toBeNull();
  });

  it("renders inline text buttons on desktop (>=md)", () => {
    useMediaQueryMock.mockReturnValue(false);
    render(<RowActions actions={actions} />);
    expect(screen.getByRole("button", { name: "Edit" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Delete" })).toBeTruthy();
    // No overflow icon button.
    expect(screen.queryByRole("button", { name: "common.moreActions" })).toBeNull();
  });

  it("calls onClick when a desktop button is clicked", () => {
    useMediaQueryMock.mockReturnValue(false);
    render(<RowActions actions={actions} />);
    fireEvent.click(screen.getByRole("button", { name: "Edit" }));
    expect(actions[0].onClick).toHaveBeenCalledTimes(1);
  });

  it("renders an overflow IconButton on narrow (<md)", () => {
    useMediaQueryMock.mockReturnValue(true);
    render(<RowActions actions={actions} />);
    expect(screen.getByRole("button", { name: "common.moreActions" })).toBeTruthy();
    // Desktop text buttons are not rendered.
    expect(screen.queryByRole("button", { name: "Edit" })).toBeNull();
  });

  it("opens the menu and invokes the action on narrow", () => {
    useMediaQueryMock.mockReturnValue(true);
    render(<RowActions actions={actions} />);
    fireEvent.click(screen.getByRole("button", { name: "common.moreActions" }));
    const item = screen.getByRole("menuitem", { name: /Delete/ });
    fireEvent.click(item);
    expect(actions[1].onClick).toHaveBeenCalledTimes(1);
    // Menu closes after selection.
    expect(screen.queryByRole("menu")).toBeNull();
  });
});
