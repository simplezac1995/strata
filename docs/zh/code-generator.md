# 代码生成器（中文摘要）

> 完整教程见 [英文代码生成器教程](../tutorials/code-generator.md)。

## 启用

```xml
<dependency>
  <groupId>io.github.simplezac1995</groupId>
  <artifactId>strata-generator</artifactId>
</dependency>
```

```yaml
strata:
  generator:
    enabled: true   # 默认关闭；prod profile 下 write 被禁用
```

## 使用

管理端 **工具 → 代码生成器**（`/tools/code-generator`）：填写目标模块、实体/表名、字段矩阵（类型、可空、可搜索、
列表/详情可见、校验）与菜单/权限，先 **Preview** 预览，再 **Download** 下载或 **Write** 写入（仅开发目录）。

HTTP 端点：`POST /api/v1/generator/preview`、`/download`、`/write`（均需权限）。

## 生成内容

后端：entity、DTO、mapper、repository、app service、controller、权限常量、demo seed、Flyway 迁移；
前端：页面组件、API client、TS 类型、路由、菜单片段、i18n key。

## 可信性

CI 三重门禁（`node scripts/verify-codegen-templates.mjs`）：生成 Java 真实编译、生成前端过 `tsc`、生成文件过严格
Prettier。模板漂移会让构建失败。

## 仍需手工接线

业务校验与错误码、角色菜单/权限种子、列表/详情字段与列宽、新端点的集成测试。
