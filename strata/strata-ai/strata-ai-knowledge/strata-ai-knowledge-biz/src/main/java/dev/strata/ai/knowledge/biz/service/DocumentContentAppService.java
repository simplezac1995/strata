// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.strata.ai.knowledge.api.entity.AiDocument;
import dev.strata.ai.knowledge.api.entity.AiDocumentChunk;
import dev.strata.ai.knowledge.biz.rag.DocumentChunker;
import dev.strata.ai.knowledge.biz.repository.AiDocumentChunkRepository;
import dev.strata.ai.knowledge.biz.repository.AiDocumentRepository;
import dev.strata.ai.knowledge.biz.repository.EmbeddingModelFinder;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.plugin.api.llm.LlmEmbedCommand;
import dev.strata.plugin.api.llm.LlmGateway;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Receives the plain-text body of a {@link AiDocument}, slices it into keyword-searchable chunks and replaces
 * any previously ingested chunks. The document row itself only holds metadata (name/type/status), so this is
 * the ingestion entry point for the RAG pipeline.
 */
@Service
public class DocumentContentAppService {

    private static final Logger log = LoggerFactory.getLogger(DocumentContentAppService.class);

    private static final ObjectMapper EMBED_MAPPER = new ObjectMapper();

    private final AiDocumentRepository documentRepository;
    private final AiDocumentChunkRepository chunkRepository;
    private final DocumentChunker chunker;
    private final EmbeddingModelFinder embeddingFinder;
    private final LlmGateway llmGateway;

    public DocumentContentAppService(
            AiDocumentRepository documentRepository,
            AiDocumentChunkRepository chunkRepository,
            DocumentChunker chunker,
            EmbeddingModelFinder embeddingFinder,
            LlmGateway llmGateway) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.chunker = chunker;
        this.embeddingFinder = embeddingFinder;
        this.llmGateway = llmGateway;
    }

    /**
     * Replace the chunks of {@code documentId} with a fresh slice of {@code text}. Returns the number of
     * chunks produced.
     */
    @Transactional
    public int attachContent(Long documentId, String text) {
        AiDocument document =
                documentRepository.findById(documentId).orElseThrow(() -> new NotFoundException("Document not found"));

        chunkRepository.deleteByDocumentId(documentId);

        List<String> slices = chunker.chunk(text);
        int index = 0;
        for (String slice : slices) {
            AiDocumentChunk chunk = new AiDocumentChunk();
            chunk.setDocumentId(documentId);
            chunk.setChunkIndex(index++);
            chunk.setContent(slice);
            chunk.setTokenCount(slice.length());
            attachEmbedding(chunk, slice);
            chunkRepository.save(chunk);
        }

        document.setChunks(slices.size());
        document.setStatus("ready");
        documentRepository.save(document);
        log.info("Ingested {} chunks for document {}", slices.size(), documentId);
        return slices.size();
    }

    private void attachEmbedding(AiDocumentChunk chunk, String text) {
        try {
            var cfg = embeddingFinder.findEnabledEmbeddingConfig().orElse(null);
            if (cfg == null) {
                return;
            }
            float[] vec = llmGateway.embed(new LlmEmbedCommand(cfg.baseUrl(), cfg.apiKey(), cfg.modelCode(), text));
            chunk.setEmbedding(EMBED_MAPPER.writeValueAsString(vec));
        } catch (Exception e) {
            log.warn(
                    "Embedding failed for chunk {}: {}; falling back to keyword search", chunk.getId(), e.getMessage());
        }
    }
}
