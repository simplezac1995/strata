// SPDX-License-Identifier: Apache-2.0
import { describe, it, expect } from "vitest";
import { errorMessage, failureMessage } from "./useCrud";
import { ApiError } from "@/shared/api/client";

describe("useCrud error utilities", () => {
  describe("errorMessage", () => {
    it("should return ApiError message when error is ApiError", () => {
      const apiError = new ApiError("Bad Request", 400, 400);
      const result = errorMessage(apiError, "fallback message");
      expect(result).toBe("Bad Request");
    });

    it("should return fallback when error is not ApiError", () => {
      const genericError = new Error("Something went wrong");
      const result = errorMessage(genericError, "fallback message");
      expect(result).toBe("fallback message");
    });

    it("should return fallback when error is null", () => {
      const result = errorMessage(null, "fallback message");
      expect(result).toBe("fallback message");
    });

    it("should return fallback when error is undefined", () => {
      const result = errorMessage(undefined, "fallback message");
      expect(result).toBe("fallback message");
    });

    it("should return fallback when error is a string", () => {
      const result = errorMessage("string error", "fallback message");
      expect(result).toBe("fallback message");
    });

    it("should handle ApiError with empty message", () => {
      const apiError = new ApiError("", 500, 500);
      const result = errorMessage(apiError, "fallback message");
      expect(result).toBe("");
    });
  });

  describe("failureMessage", () => {
    it("should return ApiError message when error is ApiError", () => {
      const apiError = new ApiError("Forbidden", 403, 403);
      const result = failureMessage(apiError);
      expect(result).toBe("Forbidden");
    });

    it("should return default fallback when error is not ApiError", () => {
      const genericError = new Error("Network error");
      const result = failureMessage(genericError);
      expect(result).toBe("toast.saveFailed");
    });

    it("should return custom fallback when provided", () => {
      const genericError = new Error("Network error");
      const result = failureMessage(genericError, "custom.fallback");
      expect(result).toBe("custom.fallback");
    });

    it("should return default fallback when error is null", () => {
      const result = failureMessage(null);
      expect(result).toBe("toast.saveFailed");
    });
  });
});
