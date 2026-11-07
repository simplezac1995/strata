// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import dev.strata.ai.knowledge.api.entity.AiDocumentChunk;
import dev.strata.ai.knowledge.biz.repository.AiDocumentChunkRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChunkSearchServiceTest {

    @Mock
    private AiDocumentChunkRepository chunkRepository;

    @Mock
    private dev.strata.ai.knowledge.biz.repository.EmbeddingModelFinder embeddingFinder;

    @Mock
    private dev.strata.plugin.api.llm.LlmGateway llmGateway;

    @InjectMocks
    private ChunkSearchService service;

    private AiDocumentChunk chunk(long id, long docId, int index, String content) {
        AiDocumentChunk c = new AiDocumentChunk();
        c.setId(id);
        c.setDocumentId(docId);
        c.setChunkIndex(index);
        c.setContent(content);
        return c;
    }

    @Test
    void tokenize_mixes_cjk_bigrams_and_english_words() {
        List<String> tokens = ChunkSearchService.tokenize("你好世界 hello");
        assertThat(tokens).contains("你好", "好世", "世界", "hello");
    }

    @Test
    void empty_query_returns_empty() {
        assertThat(service.search(List.of(1L), "  ", 5)).isEmpty();
        assertThat(service.search(List.of(1L), "hello", 0)).isEmpty();
    }

    @Test
    void search_scores_and_ranks() {
        AiDocumentChunk best = chunk(1, 10, 0, "退款流程 refund status 状态");
        AiDocumentChunk medium = chunk(2, 10, 1, "退款流程介绍");
        AiDocumentChunk none = chunk(3, 10, 2, "完全无关的内容");
        when(chunkRepository.findByDocumentIdInOrderByChunkIndexAsc(List.of(10L)))
                .thenReturn(List.of(best, medium, none));

        List<ChunkSearchService.ScoredChunk> hits = service.search(List.of(10L), "退款 refund", 5);

        assertThat(hits).hasSize(2);
        assertThat(hits.get(0).chunk()).isSameAs(best);
        assertThat(hits.get(1).chunk()).isSameAs(medium);
    }
}
