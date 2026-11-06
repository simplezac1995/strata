// SPDX-License-Identifier: Apache-2.0
/**
 * Canvas-based image processing used by the upload + crop flow.
 *
 * - `createCroppedImage`: draws the selected crop rectangle onto a canvas, scales so
 *   the longest edge is at most `maxEdgePx`, encodes as JPEG at `quality`.
 * - `createThumbnail`: draws the same crop into a square/box of `sizePx`.
 */

export interface CropArea {
  x: number;
  y: number;
  width: number;
  height: number;
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error("Failed to load image"));
    img.src = src;
  });
}

function drawCropped(
  img: HTMLImageElement,
  area: CropArea,
  targetWidth: number,
  targetHeight: number,
): HTMLCanvasElement {
  const canvas = document.createElement("canvas");
  canvas.width = Math.max(1, Math.round(targetWidth));
  canvas.height = Math.max(1, Math.round(targetHeight));
  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("Canvas 2D context unavailable");
  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = "high";
  ctx.drawImage(img, area.x, area.y, area.width, area.height, 0, 0, canvas.width, canvas.height);
  return canvas;
}

function canvasToBlob(
  canvas: HTMLCanvasElement,
  type: "image/jpeg" | "image/webp",
  quality: number,
): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => (blob ? resolve(blob) : reject(new Error("Failed to encode image"))),
      type,
      quality,
    );
  });
}

/** Crop + downscale so the longest edge <= maxEdgePx, exported as JPEG. */
export async function createCroppedImage(
  src: string,
  area: CropArea,
  maxEdgePx = 1600,
  quality = 0.85,
): Promise<Blob> {
  const img = await loadImage(src);
  let targetWidth = area.width;
  let targetHeight = area.height;
  const longest = Math.max(targetWidth, targetHeight);
  if (longest > maxEdgePx) {
    const scale = maxEdgePx / longest;
    targetWidth *= scale;
    targetHeight *= scale;
  }
  const canvas = drawCropped(img, area, targetWidth, targetHeight);
  return canvasToBlob(canvas, "image/jpeg", quality);
}

/** Produce a small thumbnail box of the cropped region. */
export async function createThumbnail(src: string, area: CropArea, sizePx = 200): Promise<Blob> {
  const img = await loadImage(src);
  const { width, height } = area;
  // Preserve the crop's aspect ratio capped to sizePx (fits inside the box).
  const scale = Math.min(1, sizePx / Math.max(width, height));
  const canvas = drawCropped(img, area, width * scale, height * scale);
  return canvasToBlob(canvas, "image/jpeg", 0.8);
}
