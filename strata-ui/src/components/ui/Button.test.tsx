// SPDX-License-Identifier: Apache-2.0
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { Button } from "./Button";

describe("Button", () => {
  it("renders children text", () => {
    render(<Button>Click me</Button>);
    expect(screen.getByText("Click me")).toBeTruthy();
  });

  it("renders with outlined variant class", () => {
    render(<Button variant="outlined">Outlined</Button>);
    const button = screen.getByText("Outlined").closest("button");
    expect(button?.className).toContain("MuiButton-outlined");
  });

  it("maps danger color to error class", () => {
    render(<Button color="danger">Danger</Button>);
    const button = screen.getByText("Danger").closest("button");
    expect(button?.className).toContain("MuiButton-colorError");
  });

  it("maps default color to inherit class", () => {
    render(<Button color="default">Default</Button>);
    const button = screen.getByText("Default").closest("button");
    expect(button?.className).toContain("MuiButton-colorInherit");
  });

  it("maps sm size to small class", () => {
    render(<Button size="sm">Small</Button>);
    const button = screen.getByText("Small").closest("button");
    expect(button?.className).toContain("MuiButton-sizeSmall");
  });

  it("maps lg size to large class", () => {
    render(<Button size="lg">Large</Button>);
    const button = screen.getByText("Large").closest("button");
    expect(button?.className).toContain("MuiButton-sizeLarge");
  });

  it("renders IconButton when variant is icon", () => {
    render(
      <Button variant="icon" aria-label="icon-btn">
        X
      </Button>,
    );
    const button = screen.getByLabelText("icon-btn");
    expect(button.className).toContain("MuiIconButton-root");
  });

  it("supports disabled attribute", () => {
    render(<Button disabled>Disabled</Button>);
    const button = screen.getByText("Disabled").closest("button");
    expect(button?.disabled).toBe(true);
  });

  it("passes custom className", () => {
    render(<Button className="my-custom-class">Custom</Button>);
    const button = screen.getByText("Custom").closest("button");
    expect(button?.className).toContain("my-custom-class");
  });

  it("uses primary color by default", () => {
    render(<Button>Default Primary</Button>);
    const button = screen.getByText("Default Primary").closest("button");
    expect(button?.className).toContain("MuiButton-colorPrimary");
  });

  it("uses contained variant by default", () => {
    render(<Button>Default Contained</Button>);
    const button = screen.getByText("Default Contained").closest("button");
    expect(button?.className).toContain("MuiButton-contained");
  });
});
