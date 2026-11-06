// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.ai.api.request.AiDocumentCreateRequest;
import dev.strata.ai.api.request.AiDocumentUpdateRequest;
import dev.strata.ai.knowledge.api.dto.AiDocumentDto;
import dev.strata.ai.knowledge.api.entity.AiDocument;
import dev.strata.ai.knowledge.biz.mapper.AiDocumentMapper;
import dev.strata.ai.knowledge.biz.repository.AiDocumentRepository;
import dev.strata.ai.knowledge.biz.repository.AiKnowledgeRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiDocumentAppService 文档")
class AiDocumentAppServiceTest {

    @Mock
    private AiDocumentRepository documentRepository;

    @Mock
    private AiKnowledgeRepository knowledgeRepository;

    @Mock
    private AiDocumentMapper mapper;

    @InjectMocks
    private AiDocumentAppService service;

    private AiDocumentDto dto() {
        return new AiDocumentDto("1", "1", "doc", 10, "pending", 0, true, null);
    }

    @Nested
    @DisplayName("查询")
    class Query {

        @Test
        @DisplayName("listByKnowledge 校验知识库存在后列出文档")
        void listByKnowledge_validates_ownership() {
            when(knowledgeRepository.existsById(5L)).thenReturn(true);
            AiDocument d = new AiDocument();
            when(documentRepository.findAllByKnowledgeIdOrderByIdAsc(5L)).thenReturn(List.of(d));
            when(mapper.toDto(d)).thenReturn(dto());

            assertThat(service.listByKnowledge(5L)).hasSize(1);
        }

        @Test
        @DisplayName("listByKnowledge 知识库不存在抛 NOT_FOUND")
        void listByKnowledge_not_found() {
            when(knowledgeRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> service.listByKnowledge(99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
            verify(documentRepository, never()).findAllByKnowledgeIdOrderByIdAsc(any());
        }

        @Test
        @DisplayName("listAll 不校验知识库，且走 Top50 上限")
        void listAll_maps() {
            AiDocument d = new AiDocument();
            when(documentRepository.findTop50ByOrderByIdAsc()).thenReturn(List.of(d));
            when(mapper.toDto(d)).thenReturn(dto());

            assertThat(service.listAll()).hasSize(1);
            verify(knowledgeRepository, never()).existsById(any());
            verify(documentRepository).findTop50ByOrderByIdAsc();
            verify(documentRepository, never()).findAllByOrderByIdAsc();
        }

        @Test
        @DisplayName("page 不带 knowledgeId 走全量分页")
        void page_without_knowledge_id_uses_find_all() {
            when(documentRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            service.page(null, 0, 10);

            verify(documentRepository).findAll(any(Pageable.class));
            verify(documentRepository, never()).findByKnowledgeId(any(), any());
        }

        @Test
        @DisplayName("page 带 knowledgeId 走按库分页")
        void page_with_knowledge_id_filters() {
            when(documentRepository.findByKnowledgeId(eq(5L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            service.page(5L, 0, 10);

            verify(documentRepository).findByKnowledgeId(eq(5L), any(Pageable.class));
            verify(documentRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("get 不存在抛 NOT_FOUND")
        void get_not_found() {
            when(documentRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(404L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("写操作")
    class Write {

        @Test
        @DisplayName("create 校验知识库后保存文档")
        void create_validates_knowledge_and_saves() {
            when(knowledgeRepository.existsById(5L)).thenReturn(true);
            AiDocumentCreateRequest request = new AiDocumentCreateRequest(5L, "doc", 10, null, 0, true);
            AiDocument entity = new AiDocument();
            when(mapper.toEntity(request)).thenReturn(entity);
            when(mapper.toDto(entity)).thenReturn(dto());

            assertThat(service.create(request).id()).isEqualTo("1");
            verify(documentRepository).save(entity);
        }

        @Test
        @DisplayName("create 知识库不存在抛 NOT_FOUND")
        void create_knowledge_missing() {
            when(knowledgeRepository.existsById(99L)).thenReturn(false);
            AiDocumentCreateRequest request = new AiDocumentCreateRequest(99L, "doc", 10, null, 0, true);

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("update 合并后保存")
        void update_saves() {
            AiDocument entity = new AiDocument();
            when(documentRepository.findById(1L)).thenReturn(Optional.of(entity));
            AiDocumentUpdateRequest request = new AiDocumentUpdateRequest("doc", 10, null, 0, true);
            when(mapper.toDto(entity)).thenReturn(dto());

            service.update(1L, request);

            verify(mapper).update(request, entity);
            verify(documentRepository).save(entity);
        }

        @Test
        @DisplayName("delete 删除存在文档")
        void delete_removes() {
            AiDocument entity = new AiDocument();
            when(documentRepository.findById(1L)).thenReturn(Optional.of(entity));

            service.delete(1L);

            verify(documentRepository).delete(entity);
        }

        @Test
        @DisplayName("delete 不存在抛 NOT_FOUND")
        void delete_not_found() {
            when(documentRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(404L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }
}
