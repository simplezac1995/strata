import type { OpenAPI3 } from "openapi-typescript";

/**
 * OpenAPI TypeScript 类型生成配置
 * 从后端 SpringDoc OpenAPI 端点生成 TypeScript 类型定义
 *
 * 使用方式：
 * 1. 启动后端服务（默认 http://localhost:8080）
 * 2. 运行 pnpm generate:api-types
 * 3. 生成的类型在 src/shared/types/api.d.ts
 */
const config = {
  // 后端 OpenAPI 文档地址
  input: "http://localhost:8080/v3/api-docs",

  // 输出文件
  output: "src/shared/types/api.d.ts",

  // 可选：如果后端需要认证，可以在这里配置 header
  // http: {
  //   headers: {
  //     Authorization: "Bearer <token>",
  //   },
  // },

  // 类型转换配置
  transform: (schemaObject: OpenAPI3.Schema) => {
    // 自定义类型转换逻辑
    return schemaObject;
  },

  // 过滤掉不需要的路径
  // pathFilter: (path) => !path.includes("/actuator"),

  // 生成的类型名称前缀
  // prefix: "",
} satisfies import("openapi-typescript").Config;

export default config;
