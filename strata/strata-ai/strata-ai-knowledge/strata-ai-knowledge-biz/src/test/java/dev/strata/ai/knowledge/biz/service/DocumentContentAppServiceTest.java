// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.ai.knowledge.api.entity.AiDocument;
import dev.strata.ai.knowledge.biz.rag.DocumentChunker;
import dev.strata.ai.knowledge.biz.repository.AiDocumentChunkRepository;
import dev.strata.ai.knowledge.biz.repository.AiDocumentRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentContentAppService 正文 ingestion")
class DocumentContentAppServiceTest {

    @Mock
    private AiDocumentRepository documentRepository;

    @Mock
    private AiDocumentChunkRepository chunkRepository;

    private DocumentContentAppService service;

    @BeforeEach
    void setUp() {
        var embeddingFinder =
                org.mockito.Mockito.mock(dev.strata.ai.knowledge.biz.repository.EmbeddingModelFinder.class);
        var llmGateway = org.mockito.Mockito.mock(dev.strata.plugin.api.llm.LlmGateway.class);
        org.mockito.Mockito.lenient()
                .when(embeddingFinder.findEnabledEmbeddingConfig())
                .thenReturn(java.util.Optional.empty());
        service = new DocumentContentAppService(
                documentRepository, chunkRepository, new DocumentChunker(500, 50), embeddingFinder, llmGateway);
    }

    @Test
    @DisplayName("文档不存在抛 NOT_FOUND")
    void document_missing_throws() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.attachContent(99L, "text"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("正文替换旧 chunk 并把文档置为 ready")
    void attach_rebuilds_chunks_and_marks_ready() {
        AiDocument doc = new AiDocument();
        when(documentRepository.findById(7L)).thenReturn(Optional.of(doc));

        int count = service.attachContent(7L, "段落一。\n段落二。");

        assertThat(count).isEqualTo(1);
        verify(chunkRepository).deleteByDocumentId(7L);
        ArgumentCaptor<AiDocument> docCap = ArgumentCaptor.forClass(AiDocument.class);
        verify(documentRepository).save(docCap.capture());
        assertThat(docCap.getValue().getStatus()).isEqualTo("ready");
        assertThat(docCap.getValue().getChunks()).isEqualTo(1);
    }
}
