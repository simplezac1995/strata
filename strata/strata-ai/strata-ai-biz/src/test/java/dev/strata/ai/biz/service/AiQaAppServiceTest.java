// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.ai.api.dto.QaRetrieveResponse;
import dev.strata.ai.api.request.QaRetrieveRequest;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.ai.config.biz.repository.AiModelRepository;
import dev.strata.ai.config.biz.repository.AiProviderRepository;
import dev.strata.ai.knowledge.api.entity.AiDocument;
import dev.strata.ai.knowledge.api.entity.AiDocumentChunk;
import dev.strata.ai.knowledge.biz.rag.ChunkSearchService;
import dev.strata.ai.knowledge.biz.repository.AiDocumentRepository;
import dev.strata.ai.knowledge.biz.repository.AiKnowledgeRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.plugin.api.llm.LlmChatCommand;
import dev.strata.plugin.api.llm.LlmChatResult;
import dev.strata.plugin.api.llm.LlmGateway;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiQaAppServiceTest {

    @Mock
    private ChunkSearchService chunkSearchService;

    @Mock
    private AiDocumentRepository documentRepository;

    @Mock
    private AiKnowledgeRepository knowledgeRepository;

    @Mock
    private LlmGateway llmGateway;

    @Mock
    private AiProviderRepository providerRepository;

    @Mock
    private AiModelRepository modelRepository;

    @Mock
    private AiUsageRecorder usageRecorder;

    @InjectMocks
    private AiQaAppService service;

    private void stubLlm() {
        AiProvider p = new AiProvider();
        p.setId(10L);
        p.setBaseUrl("https://llm");
        p.setApiKey("k");
        when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.of(p));
        AiModel m = new AiModel();
        m.setId(20L);
        m.setProviderId(10L);
        m.setCode("gpt");
        when(modelRepository.findFirstByEnabledTrueAndProviderIdOrderByIdAsc(10L))
                .thenReturn(Optional.of(m));
    }

    private AiDocumentChunk chunk(long docId, int index, String content) {
        AiDocumentChunk c = new AiDocumentChunk();
        c.setDocumentId(docId);
        c.setChunkIndex(index);
        c.setContent(content);
        return c;
    }

    @Test
    void no_provider_rejected() {
        when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retrieve(new QaRetrieveRequest("q", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
        verify(llmGateway, never()).chatWithUsage(any());
    }

    @Test
    void grounded_answer_with_citations() {
        stubLlm();
        AiDocumentChunk hit = chunk(100L, 0, "退款流程：联系客服后 3 个工作日到账。");
        when(chunkSearchService.search(any(), eq("退款"), eq(5)))
                .thenReturn(List.of(new ChunkSearchService.ScoredChunk(hit, 2)));
        AiDocument doc = new AiDocument();
        doc.setId(100L);
        doc.setName("退款手册");
        when(documentRepository.findAllById(any())).thenReturn(List.of(doc));
        when(llmGateway.chatWithUsage(any(LlmChatCommand.class)))
                .thenReturn(new LlmChatResult("3 个工作日", 5, 7, 12, "gpt", "stop"));

        QaRetrieveResponse resp = service.retrieve(new QaRetrieveRequest("退款", null));

        assertThat(resp.answer()).isEqualTo("3 个工作日");
        assertThat(resp.citations()).hasSize(1);
        assertThat(resp.citations().get(0).documentName()).isEqualTo("退款手册");
        ArgumentCaptor<LlmChatCommand> captor = ArgumentCaptor.forClass(LlmChatCommand.class);
        verify(llmGateway).chatWithUsage(captor.capture());
        assertThat(captor.getValue().userMessage()).contains("Based on the following context");
    }

    @Test
    void ungrounded_answer_when_no_hits() {
        stubLlm();
        when(chunkSearchService.search(any(), eq("天气"), eq(5))).thenReturn(List.of());
        when(llmGateway.chatWithUsage(any(LlmChatCommand.class)))
                .thenReturn(new LlmChatResult("我不知道", 3, 4, 7, "gpt", "stop"));

        QaRetrieveResponse resp = service.retrieve(new QaRetrieveRequest("天气", null));

        assertThat(resp.answer()).isEqualTo("我不知道");
        assertThat(resp.citations()).isEmpty();
    }

    @Test
    void unknown_knowledge_base_rejected() {
        stubLlm();
        when(knowledgeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.retrieve(new QaRetrieveRequest("q", 99L)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(chunkSearchService, never()).search(any(), any(), anyInt());
    }
}
