// SPDX-License-Identifier: Apache-2.0
import { apiDelete, apiGet, apiPost, apiPut, toParams } from "@/shared/api/client";
import type { PageResult, PaymentCreatedDto, PaymentDto, ProductDto } from "@/shared/types";

export interface ProductPayload {
  name: string;
  sku: string;
  price: number;
  stock: number;
  description: string | null;
  enabled: boolean;
}

export interface ProductPageQuery {
  name?: string;
  sku?: string;
  enabled?: boolean;
}

export const productApi = {
  page: (page: number, size: number, query: ProductPageQuery = {}) =>
    apiGet<PageResult<ProductDto>>(`/api/v1/demo/products${toParams({ page, size, ...query })}`),
  /** Loads everything for the client-side filtered demo table. */
  listAll: () =>
    apiGet<PageResult<ProductDto>>("/api/v1/demo/products?page=0&size=100").then((r) => r.content),
  create: (payload: ProductPayload) => apiPost<void>("/api/v1/demo/products", payload),
  update: (id: string, payload: ProductPayload) =>
    apiPut<void>(`/api/v1/demo/products/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/demo/products/${id}`),
};

export interface CreatePaymentPayload {
  amountCents: number;
  currency: string;
}

export const paymentApi = {
  list: () => apiGet<PaymentDto[]>("/api/v1/demo/payments"),
  get: (id: string) => apiGet<PaymentDto>(`/api/v1/demo/payments/${id}`),
  create: (payload: CreatePaymentPayload) =>
    apiPost<PaymentCreatedDto>("/api/v1/demo/payments", payload),
};

export const pluginApi = {
  status: () => apiGet<{ storageLocal: boolean; paymentStripe: boolean }>("/api/v1/plugins/status"),
};
