// SPDX-License-Identifier: Apache-2.0
import { describe, expect, it, beforeAll, afterEach, vi } from "vitest";
import { render, screen, fireEvent, cleanup } from "@testing-library/react";
import { ImageUpload } from "./ImageUpload";

// Cropper needs layout/pointer measurement that jsdom cannot provide; stub it
// so we can assert props flow into the crop dialog without mounting the real canvas.
vi.mock("react-easy-crop", () => ({
  __esModule: true,
  default: (props: { image: string; aspect?: number }) => (
    <div data-testid="cropper" data-image={props.image} data-aspect={props.aspect}>
      cropper
    </div>
  ),
}));

beforeAll(() => {
  // jsdom does not implement object URLs; stub so the flow can run.
  URL.createObjectURL = vi.fn(() => "blob:mock-object-url");
  URL.revokeObjectURL = vi.fn();
});

afterEach(() => cleanup());

describe("ImageUpload", () => {
  it("renders the dropzone with the label when empty", () => {
    render(<ImageUpload onChange={() => {}} />);
    expect(screen.getByTestId("image-upload-input")).toBeTruthy();
    expect(screen.getByRole("button", { name: /upload|上传/i })).toBeTruthy();
  });

  it("shows a preview and replace control when a value is present", () => {
    render(<ImageUpload value={{ url: "blob:existing" }} onChange={() => {}} />);
    const img = document.querySelector('img[alt="uploaded"]');
    expect(img).toBeTruthy();
    expect(img?.getAttribute("src")).toBe("blob:existing");
  });

  it("opens the crop dialog with the chosen image and passes aspect", () => {
    render(<ImageUpload aspect={1} onChange={() => {}} />);
    const file = new File(["fake-bytes"], "photo.png", { type: "image/png" });
    const input = screen.getByTestId("image-upload-input") as HTMLInputElement;
    fireEvent.change(input, { target: { files: [file] } });

    const cropper = screen.getByTestId("cropper");
    expect(cropper.getAttribute("data-image")).toBe("blob:mock-object-url");
    expect(cropper.getAttribute("data-aspect")).toBe("1");
  });
});
