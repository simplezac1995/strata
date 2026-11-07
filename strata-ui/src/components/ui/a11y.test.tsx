// SPDX-License-Identifier: Apache-2.0
import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Button } from "./Button";

/**
 * Accessibility smoke test: axe-core must report no violations for the shared button primitive (a building block of
 * every page). Extend this file with form/dialog/table primitives as they gain variants.
 */
describe("UI primitives accessibility", () => {
  it("Button has no detectable accessibility violations", async () => {
    const { container } = render(<Button>Save changes</Button>);
    const axe = (await import("axe-core")).default as unknown as {
      run: (context: Element) => Promise<{ violations: unknown[] }>;
    };
    const results = await axe.run(container);
    expect(results.violations).toEqual([]);
  });
});
