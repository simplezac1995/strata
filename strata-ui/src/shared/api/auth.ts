// SPDX-License-Identifier: Apache-2.0
import { apiPost } from "@/shared/api/client";
import type { UserDto } from "@/shared/types";

export interface RegisterPayload {
  username: string;
  nickname: string | null;
  email: string | null;
  password: string;
  workspace: string;
  slug: string;
}

/**
 * Result of starting TOTP enrollment (`POST /auth/mfa/enroll`). Mirrors the backend
 * `TotpEnrollResponse` record: the raw Base32 secret plus an otpauth URI and a ready-to-use
 * QR `data:` URL.
 */
export interface TotpEnrollResponse {
  secret: string;
  otpAuthUri: string;
  qrCodeDataUrl: string;
  mfaEnabled: boolean;
}

export const authApi = {
  register: (payload: RegisterPayload) => apiPost<UserDto>("/api/v1/system/auth/register", payload),
  forgotPassword: (username: string) =>
    apiPost<void>("/api/v1/system/auth/forgot-password", { username }),
  resetPassword: (token: string, newPassword: string) =>
    apiPost<void>("/api/v1/system/auth/reset-password", { token, newPassword }),
  changePassword: (oldPassword: string, newPassword: string) =>
    apiPost<void>("/api/v1/system/auth/change-password", { oldPassword, newPassword }),
  verifyEmail: (code: string) => apiPost<void>("/api/v1/system/auth/verify-email", { code }),
  /** Starts TOTP enrollment for the current admin: returns secret + otpauth URI + QR data URL. */
  mfaEnroll: () => apiPost<TotpEnrollResponse>("/api/v1/system/auth/mfa/enroll"),
  /** Confirms a pending enrollment with a valid 6-digit TOTP code. */
  mfaConfirm: (code: string) => apiPost<void>("/api/v1/system/auth/mfa/enroll/confirm", { code }),
  /** Self-service disable: proves possession with a current 6-digit TOTP code (not a password). */
  mfaDisable: (code: string) => apiPost<void>("/api/v1/system/auth/mfa/disable", { code }),
};
