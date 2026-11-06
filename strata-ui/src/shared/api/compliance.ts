// SPDX-License-Identifier: Apache-2.0
import { apiDelete, apiGet, apiPost, apiPut, toParams } from "@/shared/api/client";
import type { PageResult } from "@/shared/types";

/* ─── Compliance center DTOs (contract-aligned, do not guess fields) ────── */

/* ── KYC ── */

/** KYC application lifecycle status. */
export type KycStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface KycApplicationDto {
  id: string;
  userId: string;
  realName: string;
  idType: string;
  idNumber: string;
  idImageUrl: string;
  status: KycStatus;
  reason?: string | null;
  submittedAt: string;
  reviewedAt?: string | null;
}

export interface KycReviewPayload {
  approved: boolean;
  reason?: string;
}

export interface KycApplicationPageQuery {
  status?: string;
}

/* ── Geo restrictions ── */

/** Geo restriction action: allow or deny the matched IP. */
export type GeoRestrictionAction = "ALLOW" | "DENY";

export interface GeoRestrictionDto {
  id: string;
  ipRange: string;
  country: string;
  action: GeoRestrictionAction;
  reason?: string | null;
  enabled: boolean;
}

export interface GeoRestrictionPayload {
  ipRange: string;
  country: string;
  action: GeoRestrictionAction;
  reason?: string;
  enabled: boolean;
}

export interface GeoRegionDto {
  id: string;
  countryCode: string;
  countryName: string;
  registerBlocked: boolean;
  loginBlocked: boolean;
}

export interface GeoRegionPayload {
  countryCode: string;
  countryName: string;
  registerBlocked: boolean;
  loginBlocked: boolean;
}

export interface GeoCheckIpResult {
  ip: string;
  country?: string | null;
  blocked: boolean;
  matchedRule?: string | null;
}

/* ── Responsible gambling ── */

export type ResponsibleLimitType = "DEPOSIT" | "WAGER" | "LOSS" | "SESSION";

export interface ResponsibleUserDto {
  id: string;
  userId: string;
  limitType: ResponsibleLimitType;
  amount: number;
  period: string;
  startTime: string;
  endTime?: string | null;
}

export interface ResponsibleUserPageQuery {
  userId?: string;
}

/* ── AML ── */

/** AML rule action enum. */
export type AmlRuleAction = "REPORT" | "BLOCK" | "ALERT";

export interface AmlRuleDto {
  id: string;
  name: string;
  type: string;
  threshold: number;
  action: AmlRuleAction;
  enabled: boolean;
}

export interface AmlRulePayload {
  name: string;
  type: string;
  threshold: number;
  action: AmlRuleAction;
  enabled: boolean;
}

/** AML report lifecycle status. */
export type AmlReportStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface AmlReportDto {
  id: string;
  userId: string;
  type: string;
  amount: number;
  description?: string | null;
  status: AmlReportStatus;
  createdAt: string;
}

export interface AmlReportReviewPayload {
  approved: boolean;
  note?: string;
}

export interface AmlReportPageQuery {
  status?: string;
}

/* ── Channel limits ── */

export type ChannelCode = "TRON" | "BSC";

export type ChannelDirection = "DEPOSIT" | "WITHDRAWAL";

export interface ChannelLimitDto {
  id: string;
  channel: ChannelCode;
  direction: ChannelDirection;
  minAmount: number;
  maxAmount: number;
  dailyLimit: number;
  feeRate: number;
  enabled: boolean;
}

export interface ChannelLimitPayload {
  channel: ChannelCode;
  direction: ChannelDirection;
  minAmount: number;
  maxAmount: number;
  dailyLimit: number;
  feeRate: number;
  enabled: boolean;
}

/* ── Exchange rates ── */

export interface ExchangeRateDto {
  id: string;
  fromCurrency: string;
  toCurrency: string;
  rate: number;
  updatedAt: string;
}

export interface ExchangeRatePayload {
  fromCurrency: string;
  toCurrency: string;
  rate: number;
}

const KYC_BASE = "/api/v1/admin/kyc";
const GEO_BASE = "/api/v1/admin/geo";
const RESPONSIBLE_BASE = "/api/v1/admin/responsible";
const AML_BASE = "/api/v1/admin/aml";
const CHANNEL_LIMITS_BASE = "/api/v1/admin/channel-limits";
const EXCHANGE_RATES_BASE = "/api/v1/admin/exchange-rates";

export const complianceApi = {
  /* ── KYC ── */
  kycApplicationsPage: (query: KycApplicationPageQuery, page: number, size: number) =>
    apiGet<PageResult<KycApplicationDto>>(
      `${KYC_BASE}/applications${toParams({ ...query, page, size })}`,
    ),
  kycReview: (id: string, payload: KycReviewPayload) =>
    apiPut<KycApplicationDto>(`${KYC_BASE}/applications/${id}/review`, payload),

  /* ── Geo restrictions ── */
  geoRestrictionsList: () => apiGet<GeoRestrictionDto[]>(`${GEO_BASE}/restrictions`),
  geoRestrictionCreate: (payload: GeoRestrictionPayload) =>
    apiPost<GeoRestrictionDto>(`${GEO_BASE}/restrictions`, payload),
  geoRestrictionUpdate: (id: string, payload: GeoRestrictionPayload) =>
    apiPut<GeoRestrictionDto>(`${GEO_BASE}/restrictions/${id}`, payload),
  geoRestrictionDelete: (id: string) => apiDelete<void>(`${GEO_BASE}/restrictions/${id}`),

  /* ── Geo regions ── */
  geoRegionsList: () => apiGet<GeoRegionDto[]>(`${GEO_BASE}/regions`),
  geoRegionCreate: (payload: GeoRegionPayload) =>
    apiPost<GeoRegionDto>(`${GEO_BASE}/regions`, payload),
  geoRegionDelete: (id: string) => apiDelete<void>(`${GEO_BASE}/regions/${id}`),

  /* ── Geo check IP ── */
  geoCheckIp: (ip: string) => apiPost<GeoCheckIpResult>(`${GEO_BASE}/check-ip`, { ip }),

  /* ── Responsible gambling ── */
  responsibleUsersPage: (query: ResponsibleUserPageQuery, page: number, size: number) =>
    apiGet<PageResult<ResponsibleUserDto>>(
      `${RESPONSIBLE_BASE}/users${toParams({ ...query, page, size })}`,
    ),

  /* ── AML rules ── */
  amlRulesList: () => apiGet<AmlRuleDto[]>(`${AML_BASE}/rules`),
  amlRuleCreate: (payload: AmlRulePayload) => apiPost<AmlRuleDto>(`${AML_BASE}/rules`, payload),
  amlRuleUpdate: (id: string, payload: AmlRulePayload) =>
    apiPut<AmlRuleDto>(`${AML_BASE}/rules/${id}`, payload),
  amlRuleDelete: (id: string) => apiDelete<void>(`${AML_BASE}/rules/${id}`),

  /* ── AML reports ── */
  amlReportsPage: (query: AmlReportPageQuery, page: number, size: number) =>
    apiGet<PageResult<AmlReportDto>>(`${AML_BASE}/reports${toParams({ ...query, page, size })}`),
  amlReportReview: (id: string, payload: AmlReportReviewPayload) =>
    apiPut<AmlReportDto>(`${AML_BASE}/reports/${id}/review`, payload),

  /* ── Channel limits ── */
  channelLimitsList: () => apiGet<ChannelLimitDto[]>(`${CHANNEL_LIMITS_BASE}`),
  channelLimitCreate: (payload: ChannelLimitPayload) =>
    apiPost<ChannelLimitDto>(`${CHANNEL_LIMITS_BASE}`, payload),
  channelLimitUpdate: (id: string, payload: ChannelLimitPayload) =>
    apiPut<ChannelLimitDto>(`${CHANNEL_LIMITS_BASE}/${id}`, payload),
  channelLimitDelete: (id: string) => apiDelete<void>(`${CHANNEL_LIMITS_BASE}/${id}`),

  /* ── Exchange rates ── */
  exchangeRatesList: () => apiGet<ExchangeRateDto[]>(`${EXCHANGE_RATES_BASE}`),
  exchangeRateCreate: (payload: ExchangeRatePayload) =>
    apiPost<ExchangeRateDto>(`${EXCHANGE_RATES_BASE}`, payload),
  exchangeRateUpdate: (id: string, payload: ExchangeRatePayload) =>
    apiPut<ExchangeRateDto>(`${EXCHANGE_RATES_BASE}/${id}`, payload),
};
