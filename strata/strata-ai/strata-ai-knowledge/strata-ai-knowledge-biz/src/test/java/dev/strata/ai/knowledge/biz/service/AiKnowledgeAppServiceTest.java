// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.ai.api.request.AiKnowledgeCreateRequest;
import dev.strata.ai.api.request.AiKnowledgeUpdateRequest;
import dev.strata.ai.knowledge.api.dto.AiKnowledgeDto;
import dev.strata.ai.knowledge.api.entity.AiKnowledge;
import dev.strata.ai.knowledge.biz.mapper.AiKnowledgeMapper;
import dev.strata.ai.knowledge.biz.repository.AiDocumentRepository;
import dev.strata.ai.knowledge.biz.repository.AiKnowledgeRepository;
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
@DisplayName("AiKnowledgeAppService 知识库")
class AiKnowledgeAppServiceTest {

    @Mock
    private AiKnowledgeRepository knowledgeRepository;

    @Mock
    private AiDocumentRepository documentRepository;

    @Mock
    private AiKnowledgeMapper mapper;

    @InjectMocks
    private AiKnowledgeAppService service;

    private AiKnowledgeDto dto(String id) {
        return new AiKnowledgeDto(id, "kb", null, null, null, 512, 64, true);
    }

    @Test
    @DisplayName("list 按 id 升序映射 DTO")
    void list_maps() {
        AiKnowledge k = new AiKnowledge();
        when(knowledgeRepository.findAllByOrderByIdAsc()).thenReturn(List.of(k));
        when(mapper.toDto(k)).thenReturn(dto("1"));

        assertThat(service.list()).containsExactly(dto("1"));
    }

    @Test
    @DisplayName("page 封装分页")
    void page_maps() {
        AiKnowledge k = new AiKnowledge();
        when(knowledgeRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(k), PageRequest.of(0, 10), 1));
        when(mapper.toDto(k)).thenReturn(dto("1"));

        var result = service.page(0, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("get 存在时返回 DTO")
    void get_found() {
        AiKnowledge k = new AiKnowledge();
        when(knowledgeRepository.findById(1L)).thenReturn(Optional.of(k));
        when(mapper.toDto(k)).thenReturn(dto("1"));

        assertThat(service.get(1L).id()).isEqualTo("1");
    }

    @Test
    @DisplayName("get 不存在抛 NOT_FOUND")
    void get_not_found() {
        when(knowledgeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(404L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("create 经 mapper 转实体并保存")
    void create_saves() {
        AiKnowledge entity = new AiKnowledge();
        AiKnowledgeCreateRequest request = new AiKnowledgeCreateRequest("kb", null, null, null, 0, 0, true);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dto("1"));

        assertThat(service.create(request).id()).isEqualTo("1");
        verify(knowledgeRepository).save(entity);
    }

    @Test
    @DisplayName("update 合并后保存")
    void update_saves() {
        AiKnowledge entity = new AiKnowledge();
        when(knowledgeRepository.findById(1L)).thenReturn(Optional.of(entity));
        AiKnowledgeUpdateRequest request = new AiKnowledgeUpdateRequest("kb", null, null, null, 0, 0, true);
        when(mapper.toDto(entity)).thenReturn(dto("1"));

        assertThat(service.update(1L, request).id()).isEqualTo("1");
        verify(mapper).update(request, entity);
        verify(knowledgeRepository).save(entity);
    }

    @Test
    @DisplayName("update 不存在抛 NOT_FOUND 且不保存")
    void update_not_found() {
        when(knowledgeRepository.findById(404L)).thenReturn(Optional.empty());
        AiKnowledgeUpdateRequest request = new AiKnowledgeUpdateRequest("kb", null, null, null, 0, 0, true);

        assertThatThrownBy(() -> service.update(404L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(knowledgeRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete 级联删除文档后删除知识库")
    void delete_cascades_documents() {
        AiKnowledge entity = new AiKnowledge();
        when(knowledgeRepository.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L);

        verify(documentRepository).deleteByKnowledgeId(1L);
        verify(knowledgeRepository).delete(entity);
    }

    @Test
    @DisplayName("delete 不存在抛 NOT_FOUND")
    void delete_not_found() {
        when(knowledgeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(404L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(documentRepository, never()).deleteByKnowledgeId(any());
    }
}
