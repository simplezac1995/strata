// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.OauthClientDto;
import dev.strata.system.api.request.OauthClientCreateRequest;
import dev.strata.system.api.request.OauthClientUpdateRequest;
import dev.strata.system.rbac.api.entity.OauthClient;
import dev.strata.system.rbac.biz.mapper.OauthClientMapper;
import dev.strata.system.rbac.biz.repository.OauthClientRepository;
import java.util.List;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("OauthClientAppService 单元测试")
class OauthClientAppServiceTest {

    @Mock
    private OauthClientRepository oauthClientRepository;

    @Mock
    private OauthClientMapper oauthClientMapper;

    @InjectMocks
    private OauthClientAppService oauthClientAppService;

    private OauthClient client(long id, String clientId, String secretHash) {
        OauthClient c = new OauthClient();
        c.setId(id);
        c.setClientId(clientId);
        c.setName("client-" + clientId);
        c.setClientSecret(secretHash);
        c.setScopes("openid");
        c.setEnabled(true);
        return c;
    }

    private OauthClientDto dto() {
        return new OauthClientDto("1", "web", "web", null, "openid", true);
    }

    @Nested
    @DisplayName("list / page / get - 查询")
    class QueryTests {

        @Test
        @DisplayName("list 返回全部客户端")
        void list_MapsAll() {
            when(oauthClientRepository.findAllByOrderByIdAsc()).thenReturn(List.of(client(1L, "web", "hash")));
            when(oauthClientMapper.toDto(any(OauthClient.class))).thenReturn(dto());

            List<OauthClientDto> result = oauthClientAppService.list();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("page 分页查询")
        void page_ReturnsPaged() {
            when(oauthClientRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(client(1L, "web", "hash"))));
            when(oauthClientMapper.toDto(any(OauthClient.class))).thenReturn(dto());

            PageResult<OauthClientDto> result = oauthClientAppService.page(0, 10);

            assertThat(result.content()).hasSize(1);
        }

        @Test
        @DisplayName("get 存在时返回 DTO")
        void get_Found() {
            when(oauthClientRepository.findById(1L)).thenReturn(Optional.of(client(1L, "web", "hash")));
            when(oauthClientMapper.toDto(any(OauthClient.class))).thenReturn(dto());

            OauthClientDto result = oauthClientAppService.get(1L);

            assertThat(result.clientId()).isEqualTo("web");
        }

        @Test
        @DisplayName("get 不存在抛出 NOT_FOUND")
        void get_NotFound_Throws() {
            when(oauthClientRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oauthClientAppService.get(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("create - 创建客户端")
    class CreateTests {

        @Test
        @DisplayName("创建成功且只存哈希")
        void create_Success_StoresHash() {
            OauthClientCreateRequest request =
                    new OauthClientCreateRequest("web", "Web App", "plain-secret", "openid", true);
            OauthClient entity = client(0L, "web", "stub");
            when(oauthClientRepository.existsByClientId("web")).thenReturn(false);
            when(oauthClientMapper.toEntity(request)).thenReturn(entity);
            when(oauthClientRepository.save(entity)).thenReturn(entity);
            when(oauthClientMapper.toDto(entity)).thenReturn(dto());

            OauthClientDto result = oauthClientAppService.create(request);

            assertThat(result.clientId()).isEqualTo("web");
            assertThat(entity.getClientSecret()).isNotEqualTo("plain-secret");
            assertThat(entity.getClientSecret()).isEqualTo(DigestUtils.sha256Hex("plain-secret"));
        }

        @Test
        @DisplayName("clientId 已存在时抛出 CONFLICT")
        void create_DuplicateClientId_Throws() {
            OauthClientCreateRequest request = new OauthClientCreateRequest("dup", "Dup", "s", null, true);
            when(oauthClientRepository.existsByClientId("dup")).thenReturn(true);

            assertThatThrownBy(() -> oauthClientAppService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.CONFLICT));

            verify(oauthClientRepository, never()).save(any(OauthClient.class));
        }
    }

    @Nested
    @DisplayName("update - 更新客户端")
    class UpdateTests {

        @Test
        @DisplayName("提供新密钥时轮换哈希")
        void update_NewSecret_RotatesHash() {
            OauthClient existing = client(1L, "web", "old-hash");
            OauthClientUpdateRequest request = new OauthClientUpdateRequest("Web", "new-plain", "openid profile", true);
            when(oauthClientRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(oauthClientRepository.save(existing)).thenReturn(existing);
            when(oauthClientMapper.toDto(existing)).thenReturn(dto());

            oauthClientAppService.update(1L, request);

            assertThat(existing.getClientSecret()).isEqualTo(DigestUtils.sha256Hex("new-plain"));
            verify(oauthClientMapper).update(eq(request), any(OauthClient.class));
        }

        @Test
        @DisplayName("密钥为 null 时保持原哈希")
        void update_NullSecret_KeepsHash() {
            OauthClient existing = client(1L, "web", "old-hash");
            OauthClientUpdateRequest request = new OauthClientUpdateRequest("Web", null, null, true);
            when(oauthClientRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(oauthClientRepository.save(existing)).thenReturn(existing);
            when(oauthClientMapper.toDto(existing)).thenReturn(dto());

            oauthClientAppService.update(1L, request);

            assertThat(existing.getClientSecret()).isEqualTo("old-hash");
        }

        @Test
        @DisplayName("密钥为空白时保持原哈希")
        void update_BlankSecret_KeepsHash() {
            OauthClient existing = client(1L, "web", "old-hash");
            OauthClientUpdateRequest request = new OauthClientUpdateRequest("Web", "   ", null, true);
            when(oauthClientRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(oauthClientRepository.save(existing)).thenReturn(existing);
            when(oauthClientMapper.toDto(existing)).thenReturn(dto());

            oauthClientAppService.update(1L, request);

            assertThat(existing.getClientSecret()).isEqualTo("old-hash");
        }

        @Test
        @DisplayName("更新不存在的客户端抛出 NOT_FOUND")
        void update_NotFound_Throws() {
            OauthClientUpdateRequest request = new OauthClientUpdateRequest("x", null, null, true);
            when(oauthClientRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oauthClientAppService.update(99L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(oauthClientRepository, never()).save(any(OauthClient.class));
        }
    }

    @Nested
    @DisplayName("delete - 删除客户端")
    class DeleteTests {

        @Test
        @DisplayName("删除成功")
        void delete_Success() {
            when(oauthClientRepository.findById(1L)).thenReturn(Optional.of(client(1L, "web", "hash")));

            oauthClientAppService.delete(1L);

            verify(oauthClientRepository).delete(any(OauthClient.class));
        }

        @Test
        @DisplayName("删除不存在的客户端抛出 NOT_FOUND")
        void delete_NotFound_Throws() {
            when(oauthClientRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oauthClientAppService.delete(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(oauthClientRepository, never()).delete(any(OauthClient.class));
        }
    }
}
