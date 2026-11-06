// SPDX-License-Identifier: Apache-2.0
// Split out of CodeGenerator.tsx by scripts/auto-split.mjs.

import { type FieldSpec, type FieldType } from "@/shared/api/generator";

export const FIELD_TYPES: FieldType[] = [
  "STRING",
  "LONG",
  "INTEGER",
  "DECIMAL",
  "BOOLEAN",
  "DATETIME",
];

export type WidgetKind = "string" | "number" | "date" | "select" | "textarea" | "richtext";

export const WIDGET_OPTIONS: { value: WidgetKind; label: string }[] = [
  { value: "string", label: "string" },
  { value: "number", label: "number" },
  { value: "date", label: "date" },
  { value: "select", label: "select" },
  { value: "textarea", label: "textarea" },
  { value: "richtext", label: "richtext" },
];

export function inferWidget(field: FieldSpec): WidgetKind {
  if (field.type === "DATETIME") return "date";
  if (field.type === "LONG" || field.type === "INTEGER" || field.type === "DECIMAL")
    return "number";
  if (field.type === "BOOLEAN") return "select";
  const maxLength = field.maxLength ?? 255;
  if (maxLength >= 10000) return "richtext";
  if (maxLength >= 2000) return "textarea";
  return "string";
}

export function applyWidget(widget: WidgetKind): Partial<FieldSpec> {
  switch (widget) {
    case "number":
      return { type: "DECIMAL", maxLength: null };
    case "date":
      return { type: "DATETIME", maxLength: null };
    case "select":
      return { type: "STRING", maxLength: 64 };
    case "textarea":
      return { type: "STRING", maxLength: 2000 };
    case "richtext":
      return { type: "STRING", maxLength: 65535 };
    default:
      return { type: "STRING", maxLength: 200 };
  }
}

export interface Preset {
  tableName: string;
  entityName: string;
  module: string;
  resource: string;
  menuLabel: string;
  fields: FieldSpec[];
}

export const PRESETS: Preset[] = [
  {
    module: "demo",
    tableName: "demo_product",
    entityName: "Product",
    resource: "products",
    menuLabel: "Products",
    fields: [
      {
        name: "name",
        type: "STRING",
        required: true,
        maxLength: 200,
        queryable: true,
        unique: false,
      },
      { name: "sku", type: "STRING", required: true, maxLength: 64, queryable: true, unique: true },
      {
        name: "price",
        type: "DECIMAL",
        required: true,
        maxLength: null,
        queryable: false,
        unique: false,
      },
      {
        name: "stock",
        type: "INTEGER",
        required: false,
        maxLength: null,
        queryable: false,
        unique: false,
      },
      {
        name: "description",
        type: "STRING",
        required: false,
        maxLength: 2000,
        queryable: false,
        unique: false,
      },
      {
        name: "enabled",
        type: "BOOLEAN",
        required: true,
        maxLength: null,
        queryable: true,
        unique: false,
      },
    ],
  },
  {
    module: "system",
    tableName: "sys_role",
    entityName: "Role",
    resource: "roles",
    menuLabel: "Roles",
    fields: [
      {
        name: "code",
        type: "STRING",
        required: true,
        maxLength: 64,
        queryable: true,
        unique: true,
      },
      {
        name: "name",
        type: "STRING",
        required: true,
        maxLength: 128,
        queryable: true,
        unique: false,
      },
      {
        name: "enabled",
        type: "BOOLEAN",
        required: true,
        maxLength: null,
        queryable: true,
        unique: false,
      },
    ],
  },
];

export function clonePreset(preset: Preset): Preset {
  return { ...preset, fields: preset.fields.map((f) => ({ ...f })) };
}

export function blankField(): FieldSpec {
  return {
    name: "title",
    type: "STRING",
    required: true,
    maxLength: 200,
    queryable: true,
    unique: false,
  };
}
