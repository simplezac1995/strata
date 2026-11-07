# 升级与支持（中文摘要）

> 完整策略见 [英文升级指南](../upgrade-guide.md)。

## 版本与支持

- 0.x 遵循 SemVer 意图，minor 仍可能有破坏性变更（CHANGELOG 标注）。
- Flyway 迁移不可变（只新增文件）；Maven Central 发布不可变，tag 不可变。
- 支持窗口：最新 minor 提供安全修复；上一 minor 尽力提供补丁；更早版本请升级。
- 运行环境 Java 21+，构建工具链 JDK 25。

## 升级流程

1. 阅读目标版本之间的 CHANGELOG。
2. 升级 `strata-bom` 版本（Maven import / Gradle `platform(...)`）后重新构建。
3. 在副本上执行迁移，验证登录与核心流程。
4. 部署新镜像（Flyway 启动迁移），保留上一个镜像 tag 以便回滚。
5. 迁移已改数据时优先前滚，不要盲目回滚。

## 废弃策略

- 废弃 API 至少保留一个 minor，并在 CHANGELOG 标注。
- 数据库迁移只新增不重写。
- `*-api` 是公开契约；`*-biz` 为内部实现，minor 内可能变化。
