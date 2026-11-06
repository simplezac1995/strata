// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.ai.api.request.AiVectorStoreCreateRequest;
import dev.strata.ai.api.request.AiVectorStoreUpdateRequest;
import dev.strata.ai.knowledge.api.dto.AiVectorStoreDto;
import dev.strata.ai.knowledge.api.entity.AiVectorStore;
import dev.strata.ai.knowledge.biz.mapper.AiVectorStoreMapper;
import dev.strata.ai.knowledge.biz.repository.AiVectorStoreRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiVectorStoreAppService 向量库配置")
class AiVectorStoreAppServiceTest {

    @Mock
    private AiVectorStoreRepository repository;

    @Mock
    private AiVectorStoreMapper mapper;

    @InjectMocks
    private AiVectorStoreAppService service;

    private AiVectorStoreDto dto() {
        return new AiVectorStoreDto("1", "vs", "milvus", null, 128, true);
    }

    @Test
    @DisplayName("list 映射")
    void list_maps() {
        AiVectorStore vs = new AiVectorStore();
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(vs));
        when(mapper.toDto(vs)).thenReturn(dto());

        assertThat(service.list()).hasSize(1);
    }

    @Test
    @DisplayName("page 封装分页")
    void page_maps() {
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        assertThat(service.page(0, 10).totalElements()).isZero();
    }

    @Test
    @DisplayName("get 存在")
    void get_found() {
        AiVectorStore vs = new AiVectorStore();
        when(repository.findById(1L)).thenReturn(Optional.of(vs));
        when(mapper.toDto(vs)).thenReturn(dto());

        assertThat(service.get(1L).id()).isEqualTo("1");
    }

    @Test
    @DisplayName("get 不存在抛 NOT_FOUND")
    void get_not_found() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(404L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("create 保存")
    void create_saves() {
        AiVectorStore entity = new AiVectorStore();
        AiVectorStoreCreateRequest request = new AiVectorStoreCreateRequest("vs", "milvus", null, 128, true);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dto());

        assertThat(service.create(request).id()).isEqualTo("1");
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("update 合并保存")
    void update_saves() {
        AiVectorStore entity = new AiVectorStore();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        AiVectorStoreUpdateRequest request = new AiVectorStoreUpdateRequest("vs", "milvus", null, 128, true);
        when(mapper.toDto(entity)).thenReturn(dto());

        service.update(1L, request);

        verify(mapper).update(request, entity);
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("update 不存在抛 NOT_FOUND")
    void update_not_found() {
        when(repository.findById(404L)).thenReturn(Optional.empty());
        AiVectorStoreUpdateRequest request = new AiVectorStoreUpdateRequest("vs", "milvus", null, 128, true);

        assertThatThrownBy(() -> service.update(404L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("delete 删除")
    void delete_removes() {
        AiVectorStore entity = new AiVectorStore();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L);

        verify(repository).delete(entity);
    }
}
