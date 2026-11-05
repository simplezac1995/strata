// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.strata.ai.knowledge.api.entity.AiDocumentChunk;
import dev.strata.ai.knowledge.biz.repository.AiDocumentChunkRepository;
import dev.strata.ai.knowledge.biz.repository.EmbeddingModelFinder;
import dev.strata.plugin.api.llm.LlmEmbedCommand;
import dev.strata.plugin.api.llm.LlmGateway;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keyword retrieval over {@link AiDocumentChunk#content} for the Community RAG MVP.
 *
 * <p>No vector store this iteration: the query is tokenized into Chinese bigrams (each adjacent CJK pair) plus
 * whitespace-separated English/numeric words, then every candidate chunk is scored by how many of those tokens
 * it contains (case-insensitive substring match). The top-K chunks are returned ordered by score, then by
 * chunk index. It works on PostgreSQL without FULLTEXT or pg_trgm extensions.
 */
@Service
public class ChunkSearchService {

    private static final ObjectMapper VEC_MAPPER = new ObjectMapper();

    private final AiDocumentChunkRepository chunkRepository;
    private final EmbeddingModelFinder embeddingFinder;
    private final LlmGateway llmGateway;

    public ChunkSearchService(
            AiDocumentChunkRepository chunkRepository, EmbeddingModelFinder embeddingFinder, LlmGateway llmGateway) {
        this.chunkRepository = chunkRepository;
        this.embeddingFinder = embeddingFinder;
        this.llmGateway = llmGateway;
    }

    /** One retrieval hit: the chunk plus its raw token-hit score. */
    public record ScoredChunk(AiDocumentChunk chunk, int score) {}

    /**
     * Search the given document ids (empty/null means "all ingested chunks", capped at 500 rows) for the top
     * {@code topK} chunks matching {@code query}. Returns chunks only when at least one token hits.
     */
    @Transactional(readOnly = true)
    public List<ScoredChunk> search(Collection<Long> documentIds, String query, int topK) {
        List<String> tokens = tokenize(query);
        if (tokens.isEmpty() || topK <= 0) {
            return List.of();
        }
        List<AiDocumentChunk> candidates = documentIds == null || documentIds.isEmpty()
                ? chunkRepository.findTop500ByOrderByIdAsc()
                : chunkRepository.findByDocumentIdInOrderByChunkIndexAsc(documentIds);

        float[] queryVec = null;
        try {
            var cfg = embeddingFinder.findEnabledEmbeddingConfig().orElse(null);
            if (cfg != null) {
                queryVec = llmGateway.embed(new LlmEmbedCommand(cfg.baseUrl(), cfg.apiKey(), cfg.modelCode(), query));
            }
        } catch (Exception e) {
            queryVec = null;
        }

        List<ScoredChunk> scored = new ArrayList<>();
        for (AiDocumentChunk chunk : candidates) {
            int score;
            if (queryVec != null
                    && chunk.getEmbedding() != null
                    && !chunk.getEmbedding().isBlank()) {
                score = (int) Math.round(cosineScore(queryVec, chunk.getEmbedding()) * 1000);
            } else {
                score = score(chunk.getContent(), tokens);
            }
            if (score > 0) {
                scored.add(new ScoredChunk(chunk, score));
            }
        }
        scored.sort(Comparator.comparingInt(ScoredChunk::score).reversed().thenComparing(c -> c.chunk()
                .getChunkIndex()));
        return scored.size() <= topK ? scored : scored.subList(0, topK);
    }

    /**
     * Split the query into retrieval tokens: adjacent CJK pairs (2-grams) plus contiguous ASCII word/numeric
     * runs. Punctuation, spaces and single CJK characters that do not form a pair are dropped.
     */
    static List<String> tokenize(String query) {
        Set<String> tokens = new LinkedHashSet<>();
        if (query == null) {
            return List.of();
        }
        String lower = query.toLowerCase(Locale.ROOT);
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (isCjk(c)) {
                if (word.length() > 0) {
                    tokens.add(word.toString());
                    word.setLength(0);
                }
                if (i + 1 < lower.length() && isCjk(lower.charAt(i + 1))) {
                    tokens.add(lower.substring(i, i + 2));
                }
            } else if (Character.isLetterOrDigit(c)) {
                word.append(c);
            } else {
                if (word.length() > 0) {
                    tokens.add(word.toString());
                    word.setLength(0);
                }
            }
        }
        if (word.length() > 0) {
            tokens.add(word.toString());
        }
        return new ArrayList<>(tokens);
    }

    private static int score(String content, List<String> tokens) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        String haystack = content.toLowerCase(Locale.ROOT);
        int hits = 0;
        for (String token : tokens) {
            if (haystack.contains(token)) {
                hits++;
            }
        }
        return hits;
    }

    private static double cosineScore(float[] a, String bJson) {
        try {
            float[] b = VEC_MAPPER.readValue(bJson, float[].class);
            if (a.length != b.length) return 0;
            double dot = 0, na = 0, nb = 0;
            for (int i = 0; i < a.length; i++) {
                dot += a[i] * b[i];
                na += a[i] * a[i];
                nb += b[i] * b[i];
            }
            if (na == 0 || nb == 0) return 0;
            return dot / (Math.sqrt(na) * Math.sqrt(nb));
        } catch (Exception e) {
            return 0;
        }
    }

    private static boolean isCjk(char c) {
        return c >= '一' && c <= '鿿';
    }
}
