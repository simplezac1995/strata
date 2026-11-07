// SPDX-License-Identifier: Apache-2.0
import { apiPost, apiPostBlob } from "@/shared/api/client";

export type FieldType = "STRING" | "LONG" | "INTEGER" | "DECIMAL" | "BOOLEAN" | "DATETIME";

export interface FieldSpec {
  name: string;
  type: FieldType;
  required: boolean;
  maxLength: number | null;
  queryable: boolean;
  unique: boolean;
}

export interface GeneratorRequest {
  module: string;
  tableName: string;
  entityName: string;
  resource: string;
  menuLabel: string;
  basePackage?: string;
  fields: FieldSpec[];
}

export interface GeneratorFile {
  path: string;
  content: string;
}

export const generatorApi = {
  preview: (body: GeneratorRequest) => apiPost<GeneratorFile[]>("/api/v1/generator/preview", body),
  download: (body: GeneratorRequest) => apiPostBlob("/api/v1/generator/download", body),
  write: (body: GeneratorRequest) => apiPost<string[]>("/api/v1/generator/write", body),
};
