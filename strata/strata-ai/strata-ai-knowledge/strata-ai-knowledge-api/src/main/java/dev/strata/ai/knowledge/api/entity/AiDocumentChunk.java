// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.api.entity;

import dev.strata.common.core.util.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One text slice of a {@link AiDocument} ({@code ai_document_chunk}) used for keyword (RAG) retrieval. The
 * content is sliced by paragraph with a ~500-character window and 50-character overlap; vector columns are
 * intentionally absent — retrieval is token-scored LIKE matching in the application layer this iteration.
 */
@Entity
@Table(
        name = "ai_document_chunk",
        indexes = {
            @Index(name = "idx_ai_doc_chunk_document", columnList = "document_id"),
            @Index(name = "idx_ai_doc_chunk_doc_idx", columnList = "document_id, chunk_index")
        })
public class AiDocumentChunk {

    @Id
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "token_count", nullable = false)
    private int tokenCount;

    /** Serialized float[] embedding (JSON). Null for chunks indexed before vector search. */
    @Column(columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = IdGenerator.next();
        }
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
