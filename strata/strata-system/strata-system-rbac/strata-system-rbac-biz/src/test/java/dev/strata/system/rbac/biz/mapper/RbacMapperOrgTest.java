// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.system.api.dto.DeptDto;
import dev.strata.system.api.dto.OauthClientDto;
import dev.strata.system.api.request.DeptCreateRequest;
import dev.strata.system.api.request.DeptUpdateRequest;
import dev.strata.system.api.request.OauthClientCreateRequest;
import dev.strata.system.api.request.OauthClientUpdateRequest;
import dev.strata.system.api.request.TenantCreateRequest;
import dev.strata.system.api.request.TenantUpdateRequest;
import dev.strata.system.rbac.api.entity.Dept;
import dev.strata.system.rbac.api.entity.OauthClient;
import dev.strata.system.rbac.api.entity.Tenant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 组织与租户侧 Mapper 接口契约单元测试：直接实例化 MapStruct 生成实现，覆盖 toEntity/toDto/update 中的
 * null-check 与 defaultValue 分支。不启动 Spring 容器。
 *
 * <p>本类覆盖部门 / 租户 / OAuth 客户端三个 Mapper；用户 / 角色 / 菜单见 {@link RbacMapperTest}。
 */
class RbacMapperOrgTest {

    private final DeptMapper deptMapper = new DeptMapperImpl();
    private final TenantMapper tenantMapper = new TenantMapperImpl();
    private final OauthClientMapper oauthClientMapper = new OauthClientMapperImpl();

    @Nested
    @DisplayName("DeptMapper")
    class DeptMapperCases {

        @Test
        @DisplayName("toEntity(null) 返回 null")
        void toEntityNull() {
            assertThat(deptMapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("toEntity parentId 数字字符串与 null 分支")
        void toEntityParentId() {
            assertThat(deptMapper
                            .toEntity(new DeptCreateRequest("1", "研发部", 1, true))
                            .getParentId())
                    .isEqualTo(1L);
            assertThat(deptMapper
                            .toEntity(new DeptCreateRequest(null, "研发部", 1, true))
                            .getParentId())
                    .isNull();
            assertThat(deptMapper
                            .toEntity(new DeptCreateRequest(" ", "研发部", 1, true))
                            .getParentId())
                    .isNull();
        }

        @Test
        @DisplayName("toDto(null) 返回 null")
        void toDtoNull() {
            assertThat(deptMapper.toDto(null)).isNull();
        }

        @Test
        @DisplayName("toDto 全字段映射与 null id/parentId 分支")
        void toDtoBranches() {
            Dept dept = new Dept();
            dept.setId(5L);
            dept.setParentId(2L);
            dept.setName("研发部");
            dept.setSortOrder(3);
            dept.setEnabled(true);
            DeptDto dto = deptMapper.toDto(dept);
            assertThat(dto.id()).isEqualTo("5");
            assertThat(dto.parentId()).isEqualTo("2");
            assertThat(dto.name()).isEqualTo("研发部");
            assertThat(dto.sortOrder()).isEqualTo(3);
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.children()).isEmpty();

            dept.setId(null);
            dept.setParentId(null);
            DeptDto nullIds = deptMapper.toDto(dept);
            assertThat(nullIds.id()).isNull();
            assertThat(nullIds.parentId()).isNull();
        }

        @Test
        @DisplayName("update(null, 部门) 不改；可空字段 null 时保持 parentId/name")
        void updateBranches() {
            Dept dept = new Dept();
            dept.setParentId(2L);
            dept.setName("旧名");
            dept.setSortOrder(1);
            dept.setEnabled(true);
            deptMapper.update(null, dept);
            assertThat(dept.getName()).isEqualTo("旧名");

            deptMapper.update(new DeptUpdateRequest("20", "新名", 9, false), dept);
            assertThat(dept.getName()).isEqualTo("新名");
            assertThat(dept.getSortOrder()).isEqualTo(9);
            assertThat(dept.isEnabled()).isFalse();

            deptMapper.update(new DeptUpdateRequest(null, null, 4, true), dept);
            // parentId 为 null 时不更新（IGNORE）
            assertThat(dept.getParentId()).isEqualTo(20L);
            assertThat(dept.getName()).isEqualTo("新名");
            assertThat(dept.getSortOrder()).isEqualTo(4);
            assertThat(dept.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("TenantMapper")
    class TenantMapperCases {

        @Test
        @DisplayName("toEntity(null) 返回 null")
        void toEntityNull() {
            assertThat(tenantMapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("toEntity 全字段映射")
        void toEntityFull() {
            Tenant tenant =
                    tenantMapper.toEntity(new TenantCreateRequest("acme", "Acme", "acme.example.com", "active", "pro"));
            assertThat(tenant.getCode()).isEqualTo("acme");
            assertThat(tenant.getName()).isEqualTo("Acme");
            assertThat(tenant.getDomain()).isEqualTo("acme.example.com");
            assertThat(tenant.getStatus()).isEqualTo("active");
            assertThat(tenant.getPlan()).isEqualTo("pro");
        }

        @Test
        @DisplayName("toDto(null) 返回 null；全字段与 null id 分支")
        void toDtoBranches() {
            assertThat(tenantMapper.toDto(null)).isNull();

            Tenant tenant = new Tenant();
            tenant.setId(11L);
            tenant.setCode("acme");
            tenant.setName("Acme");
            tenant.setDomain("acme.example.com");
            tenant.setStatus("active");
            tenant.setPlan("pro");
            var dto = tenantMapper.toDto(tenant);
            assertThat(dto.id()).isEqualTo("11");
            assertThat(dto.code()).isEqualTo("acme");

            tenant.setId(null);
            assertThat(tenantMapper.toDto(tenant).id()).isNull();
        }

        @Test
        @DisplayName("update(null, 租户) 不改；全字段直接覆盖")
        void updateBranches() {
            Tenant tenant = new Tenant();
            tenantMapper.update(null, tenant);
            assertThat(tenant.getName()).isNull();

            tenantMapper.update(new TenantUpdateRequest("NewName", "new.example.com", "suspended", "free"), tenant);
            assertThat(tenant.getName()).isEqualTo("NewName");
            assertThat(tenant.getDomain()).isEqualTo("new.example.com");
            assertThat(tenant.getStatus()).isEqualTo("suspended");
            assertThat(tenant.getPlan()).isEqualTo("free");
        }
    }

    @Nested
    @DisplayName("OauthClientMapper")
    class OauthClientMapperCases {

        @Test
        @DisplayName("toEntity(null) 返回 null")
        void toEntityNull() {
            assertThat(oauthClientMapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("toEntity enabled 显式非空时使用请求值")
        void toEntityEnabledExplicit() {
            OauthClient client = oauthClientMapper.toEntity(
                    new OauthClientCreateRequest("cid", "name", "secret", "read write", false));
            assertThat(client.getClientId()).isEqualTo("cid");
            assertThat(client.getName()).isEqualTo("name");
            assertThat(client.getClientSecret()).isEqualTo("secret");
            assertThat(client.getScopes()).isEqualTo("read write");
            assertThat(client.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("toEntity enabled 为 null 时默认 true（defaultValue 分支）")
        void toEntityEnabledDefault() {
            OauthClient client =
                    oauthClientMapper.toEntity(new OauthClientCreateRequest("cid", "name", "secret", null, null));
            assertThat(client.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("toDto(null) 返回 null；全字段与 null id 分支；secret 始终忽略")
        void toDtoBranches() {
            assertThat(oauthClientMapper.toDto(null)).isNull();

            OauthClient client = new OauthClient();
            client.setId(9L);
            client.setClientId("cid");
            client.setName("name");
            client.setScopes("read");
            client.setEnabled(true);
            OauthClientDto dto = oauthClientMapper.toDto(client);
            assertThat(dto.id()).isEqualTo("9");
            assertThat(dto.clientId()).isEqualTo("cid");
            assertThat(dto.name()).isEqualTo("name");
            assertThat(dto.scopes()).isEqualTo("read");
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.clientSecret()).isNull();

            client.setId(null);
            assertThat(oauthClientMapper.toDto(client).id()).isNull();
        }

        @Test
        @DisplayName("update(null, 客户端) 不改；可空字段 null 时保持原值")
        void updateBranches() {
            OauthClient client = new OauthClient();
            client.setName("old");
            client.setScopes("read");
            client.setEnabled(true);

            oauthClientMapper.update(null, client);
            assertThat(client.getName()).isEqualTo("old");

            oauthClientMapper.update(new OauthClientUpdateRequest("new", "s", "write", false), client);
            assertThat(client.getName()).isEqualTo("new");
            assertThat(client.getScopes()).isEqualTo("write");
            assertThat(client.isEnabled()).isFalse();

            oauthClientMapper.update(new OauthClientUpdateRequest(null, null, null, null), client);
            assertThat(client.getName()).isEqualTo("new");
            assertThat(client.getScopes()).isEqualTo("write");
            assertThat(client.isEnabled()).isFalse();
        }
    }
}
