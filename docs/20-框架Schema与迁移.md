# 框架 Schema 与数据库迁移

> 开源框架如何交付数据库结构，消费者如何维护自己的迁移，以及实体变更后如何再生成框架迁移。

## 1. 交付方式

框架 schema 随 starter 发布：

```
strata-spring-boot-starter/src/main/resources/db/strata-framework/V1__strata_framework.sql
```

该文件覆盖 system / common / AI 全部 75 张表，由 JPA 实体映射**自动生成**，避免手写 DDL 与实体漂移。
消费者在 `application.yml` 中保留并追加自己的迁移位置：

```yaml
spring:
  flyway:
    locations: classpath:db/strata-framework,classpath:db/migration
```

私有产品仓库有自己的 Flyway 迁移集（含业务表），导出开源仓时 starter 会自动移除对私有
`strata-migrations` 的依赖。

## 2. 消费者自己的迁移

- 表名前缀不与框架冲突即可（框架表：`sys_*` / `ai_*` 等）；
- 迁移文件按 Flyway 规则命名（`V2__xxx.sql`），永不修改已发布迁移；
- 本地快速体验也可以 `spring.jpa.hibernate.ddl-auto=create-drop` + `spring.flyway.enabled=false`，
  但只适合一次性 demo。

## 3. 修改实体后重新生成框架迁移

```bash
cd strata
mvn -o -B -pl examples/strata-minimal-app -am -Dtest=FrameworkSchemaExportTest test
cp examples/strata-minimal-app/target/framework-schema.sql \
   strata-starter/strata-spring-boot-starter/src/main/resources/db/strata-framework/V1__strata_framework.sql
```

导出脚本会去掉 `public.` schema 前缀，使迁移在消费者自己的 schema 中可用。生成结果由
`MinimalApplicationSmokeTest` 用 Hibernate `validate` 在独立数据库中验证；CI 的
`FrameworkSchemaExportTest` 会拒绝重复/过期的 DDL。

## 4. 版本升级注意

| 场景 | 做法 |
| :--- | :--- |
| 框架小版本新增表/列 | starter 内新增 `V2__...` 迁移，消费者升级 starter 后自动执行 |
| 已有表结构变更 | 永远新增迁移文件（不可变原则），在文档中标注最低框架版本 |
| 消费者自建表与框架表同名 | 通过 `spring.flyway.table` 或独立 schema 隔离 |
