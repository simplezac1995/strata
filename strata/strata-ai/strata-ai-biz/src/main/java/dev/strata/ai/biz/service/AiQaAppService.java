// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.service;

import dev.strata.ai.api.dto.QaRetrieveResponse;
import dev.strata.ai.api.dto.QaRetrieveResponse.Citation;
import dev.strata.ai.api.request.QaRetrieveRequest;
import dev.strata.ai.chat.biz.service.LlmGatewaySupport;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.ai.config.biz.repository.AiModelRepository;
import dev.strata.ai.config.biz.repository.AiProviderRepository;
import dev.strata.ai.knowledge.api.entity.AiDocument;
import dev.strata.ai.knowledge.biz.rag.ChunkSearchService;
import dev.strata.ai.knowledge.biz.rag.ChunkSearchService.ScoredChunk;
import dev.strata.ai.knowledge.biz.repository.AiDocumentRepository;
import dev.strata.ai.knowledge.biz.repository.AiKnowledgeRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.plugin.api.llm.LlmChatResult;
import dev.strata.plugin.api.llm.LlmGateway;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grounded QA over ingested document chunks (Community RAG MVP).
 *
 * <p>Retrieval is keyword (token-scored, no vector store this iteration): the question is tokenized into
 * Chinese bigrams + English words, the top-5 chunks are pulled from the requested knowledge base (or globally
 * when {@code knowledgeBaseId} is null), and the model is asked to answer from the assembled context and say
 * it does not know when the context is silent. When nothing hits, the question is answered directly, ungrounded.
 */
@Service
public class AiQaAppService {

    private static final int TOP_K = 5;
    private static final int SNIPPET_LENGTH = 200;

    private final ChunkSearchService chunkSearchService;
    private final AiDocumentRepository documentRepository;
    private final AiKnowledgeRepository knowledgeRepository;
    private final LlmGateway llmGateway;
    private final AiProviderRepository aiProviderRepository;
    private final AiModelRepository aiModelRepository;
    private final AiUsageRecorder usageRecorder;

    public AiQaAppService(
            ChunkSearchService chunkSearchService,
            AiDocumentRepository documentRepository,
            AiKnowledgeRepository knowledgeRepository,
            LlmGateway llmGateway,
            AiProviderRepository aiProviderRepository,
            AiModelRepository aiModelRepository,
            AiUsageRecorder usageRecorder) {
        this.chunkSearchService = chunkSearchService;
        this.documentRepository = documentRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.llmGateway = llmGateway;
        this.aiProviderRepository = aiProviderRepository;
        this.aiModelRepository = aiModelRepository;
        this.usageRecorder = usageRecorder;
    }

    @Transactional(readOnly = true)
    public QaRetrieveResponse retrieve(QaRetrieveRequest request) {
        String question = request.question() == null ? "" : request.question().trim();
        if (question.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Question must not be blank");
        }
        ResolvedLlm llm = resolveLlm();

        List<Long> documentIds = scopeToKnowledgeBase(request.knowledgeBaseId());
        List<ScoredChunk> hits = chunkSearchService.search(documentIds, question, TOP_K);

        String prompt = hits.isEmpty() ? question : buildGroundedPrompt(question, hits);

        LlmChatResult result =
                llmGateway.chatWithUsage(LlmGatewaySupport.toCommand(llm.provider(), llm.model(), List.of(), prompt));
        String answer = result.content();
        usageRecorder.record(llm.provider().getCode(), llm.model().getCode(), null, "qa", result);

        return new QaRetrieveResponse(answer, buildCitations(hits));
    }

    /**
     * Resolve the set of document ids to search: when {@code knowledgeBaseId} is supplied the knowledge base
     * must exist and search is restricted to its documents; otherwise search runs across all ingested chunks.
     */
    private List<Long> scopeToKnowledgeBase(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return null;
        }
        if (!knowledgeRepository.existsById(knowledgeBaseId)) {
            throw new NotFoundException("Knowledge base not found");
        }
        return documentRepository.findAllByKnowledgeIdOrderByIdAsc(knowledgeBaseId).stream()
                .map(AiDocument::getId)
                .toList();
    }

    private static String buildGroundedPrompt(String question, List<ScoredChunk> hits) {
        StringBuilder context = new StringBuilder();
        for (ScoredChunk hit : hits) {
            context.append("--- chunk ")
                    .append(hit.chunk().getChunkIndex())
                    .append(" ---\n")
                    .append(hit.chunk().getContent().trim())
                    .append("\n");
        }
        return "Based on the following context, answer the question. If the context doesn't contain the answer, "
                + "say you don't know.\n\nContext:\n"
                + context
                + "\nQuestion: "
                + question;
    }

    private List<Citation> buildCitations(List<ScoredChunk> hits) {
        if (hits.isEmpty()) {
            return List.of();
        }
        Set<Long> docIds = new LinkedHashSet<>();
        for (ScoredChunk hit : hits) {
            docIds.add(hit.chunk().getDocumentId());
        }
        Map<Long, AiDocument> documents = documentRepository.findAllById(docIds).stream()
                .collect(Collectors.toMap(AiDocument::getId, Function.identity()));

        List<Citation> citations = new ArrayList<>();
        for (ScoredChunk hit : hits) {
            AiDocument doc = documents.get(hit.chunk().getDocumentId());
            citations.add(new Citation(
                    hit.chunk().getDocumentId(),
                    doc == null ? null : doc.getName(),
                    hit.chunk().getChunkIndex(),
                    snippet(hit.chunk().getContent())));
        }
        return citations;
    }

    private static String snippet(String content) {
        if (content == null) {
            return "";
        }
        String flat = content.replaceAll("\\s+", " ").trim();
        return flat.length() <= SNIPPET_LENGTH ? flat : flat.substring(0, SNIPPET_LENGTH) + "…";
    }

    private ResolvedLlm resolveLlm() {
        AiProvider provider = aiProviderRepository
                .findFirstByEnabledTrueOrderByIdAsc()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "No enabled AI provider configured"));
        AiModel model = aiModelRepository
                .findFirstByEnabledTrueAndProviderIdOrderByIdAsc(provider.getId())
                .or(() -> aiModelRepository.findFirstByEnabledTrueOrderByIdAsc())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "No enabled AI model configured"));
        return new ResolvedLlm(provider, model);
    }

    private record ResolvedLlm(AiProvider provider, AiModel model) {}
}
