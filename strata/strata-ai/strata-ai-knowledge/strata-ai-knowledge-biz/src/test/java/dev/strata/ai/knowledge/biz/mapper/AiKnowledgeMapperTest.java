// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.ai.api.request.AiDatasetCreateRequest;
import dev.strata.ai.api.request.AiDatasetUpdateRequest;
import dev.strata.ai.api.request.AiDocumentCreateRequest;
import dev.strata.ai.api.request.AiDocumentUpdateRequest;
import dev.strata.ai.api.request.AiKnowledgeCreateRequest;
import dev.strata.ai.api.request.AiKnowledgeUpdateRequest;
import dev.strata.ai.api.request.AiVectorStoreCreateRequest;
import dev.strata.ai.api.request.AiVectorStoreUpdateRequest;
import dev.strata.ai.knowledge.api.dto.AiDatasetDto;
import dev.strata.ai.knowledge.api.dto.AiDocumentDto;
import dev.strata.ai.knowledge.api.dto.AiKnowledgeDto;
import dev.strata.ai.knowledge.api.dto.AiVectorStoreDto;
import dev.strata.ai.knowledge.api.entity.AiDataset;
import dev.strata.ai.knowledge.api.entity.AiDocument;
import dev.strata.ai.knowledge.api.entity.AiKnowledge;
import dev.strata.ai.knowledge.api.entity.AiVectorStore;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * AI Knowledge 模块 Mapper 接口契约单元测试：覆盖 null-check 与 defaultValue 分支，不启动 Spring 容器。
 */
class AiKnowledgeMapperTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final AiKnowledgeMapper knowledgeMapper = new AiKnowledgeMapperImpl();
    private final AiDocumentMapper documentMapper = new AiDocumentMapperImpl();
    private final AiDatasetMapper datasetMapper = new AiDatasetMapperImpl();
    private final AiVectorStoreMapper vectorStoreMapper = new AiVectorStoreMapperImpl();

    @Nested
    @DisplayName("AiKnowledgeMapper")
    class KnowledgeCases {

        @Test
        @DisplayName("toEntity(null) 返回 null")
        void toEntityNull() {
            assertThat(knowledgeMapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("toEntity enabled 显式非空时使用请求值")
        void toEntityExplicit() {
            AiKnowledge k = knowledgeMapper.toEntity(
                    new AiKnowledgeCreateRequest("产品手册", "desc", "bge-m3", "pgvector", 512, 50, false));
            assertThat(k.getName()).isEqualTo("产品手册");
            assertThat(k.getDescription()).isEqualTo("desc");
            assertThat(k.getEmbeddingModel()).isEqualTo("bge-m3");
            assertThat(k.getVectorStore()).isEqualTo("pgvector");
            assertThat(k.getChunkSize()).isEqualTo(512);
            assertThat(k.getChunkOverlap()).isEqualTo(50);
            assertThat(k.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("toEntity enabled 为 null 时默认 true")
        void toEntityDefaultEnabled() {
            AiKnowledge k =
                    knowledgeMapper.toEntity(new AiKnowledgeCreateRequest("产品手册", null, null, null, 0, 0, null));
            assertThat(k.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("toDto(null) 返回 null；全字段与 null id 分支")
        void toDtoBranches() {
            assertThat(knowledgeMapper.toDto(null)).isNull();

            AiKnowledge k = new AiKnowledge();
            k.setId(1L);
            k.setName("产品手册");
            k.setDescription("desc");
            k.setEmbeddingModel("bge-m3");
            k.setVectorStore("pgvector");
            k.setChunkSize(512);
            k.setChunkOverlap(50);
            k.setEnabled(true);
            AiKnowledgeDto dto = knowledgeMapper.toDto(k);
            assertThat(dto.id()).isEqualTo("1");
            assertThat(dto.name()).isEqualTo("产品手册");
            assertThat(dto.description()).isEqualTo("desc");
            assertThat(dto.embeddingModel()).isEqualTo("bge-m3");
            assertThat(dto.vectorStore()).isEqualTo("pgvector");
            assertThat(dto.chunkSize()).isEqualTo(512);
            assertThat(dto.chunkOverlap()).isEqualTo(50);
            assertThat(dto.enabled()).isTrue();

            k.setId(null);
            assertThat(knowledgeMapper.toDto(k).id()).isNull();
        }

        @Test
        @DisplayName("update(null, 知识) 不改；可空字段 null 时保持原值")
        void updateBranches() {
            AiKnowledge k = new AiKnowledge();
            k.setName("old");
            k.setDescription("old-desc");
            k.setEmbeddingModel("old-model");
            k.setVectorStore("old-store");
            k.setChunkSize(1);
            k.setChunkOverlap(2);
            k.setEnabled(true);

            knowledgeMapper.update(null, k);
            assertThat(k.getName()).isEqualTo("old");

            knowledgeMapper.update(
                    new AiKnowledgeUpdateRequest("new", "new-desc", "new-model", "new-store", 16, 8, false), k);
            assertThat(k.getName()).isEqualTo("new");
            assertThat(k.getDescription()).isEqualTo("new-desc");
            assertThat(k.getEmbeddingModel()).isEqualTo("new-model");
            assertThat(k.getVectorStore()).isEqualTo("new-store");
            assertThat(k.getChunkSize()).isEqualTo(16);
            assertThat(k.getChunkOverlap()).isEqualTo(8);
            assertThat(k.isEnabled()).isFalse();

            knowledgeMapper.update(new AiKnowledgeUpdateRequest(null, null, null, null, 32, 4, null), k);
            assertThat(k.getName()).isEqualTo("new");
            assertThat(k.getChunkSize()).isEqualTo(32);
            assertThat(k.getChunkOverlap()).isEqualTo(4);
            assertThat(k.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("AiDocumentMapper")
    class DocumentCases {

        @Test
        @DisplayName("toEntity(null) 返回 null")
        void toEntityNull() {
            assertThat(documentMapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("toEntity status/enabled 显式非空时使用请求值")
        void toEntityExplicit() {
            AiDocument doc =
                    documentMapper.toEntity(new AiDocumentCreateRequest(7L, "doc.pdf", 1024, "failed", 12, false));
            assertThat(doc.getKnowledgeId()).isEqualTo(7L);
            assertThat(doc.getName()).isEqualTo("doc.pdf");
            assertThat(doc.getSizeKb()).isEqualTo(1024);
            assertThat(doc.getStatus()).isEqualTo("failed");
            assertThat(doc.getChunks()).isEqualTo(12);
            assertThat(doc.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("toEntity status/enabled 为 null 时走 defaultValue 分支")
        void toEntityDefaults() {
            AiDocument doc = documentMapper.toEntity(new AiDocumentCreateRequest(7L, "doc.pdf", 0, null, 0, null));
            assertThat(doc.getStatus()).isEqualTo("pending");
            assertThat(doc.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("toDto(null) 返回 null；全字段与 null id/knowledgeId 分支")
        void toDtoBranches() {
            assertThat(documentMapper.toDto(null)).isNull();

            AiDocument doc = new AiDocument();
            doc.setId(3L);
            doc.setKnowledgeId(7L);
            doc.setName("doc.pdf");
            doc.setSizeKb(1024);
            doc.setStatus("ready");
            doc.setChunks(12);
            doc.setEnabled(true);
            doc.setCreatedAt(NOW);
            AiDocumentDto dto = documentMapper.toDto(doc);
            assertThat(dto.id()).isEqualTo("3");
            assertThat(dto.knowledgeId()).isEqualTo("7");
            assertThat(dto.name()).isEqualTo("doc.pdf");
            assertThat(dto.sizeKb()).isEqualTo(1024);
            assertThat(dto.status()).isEqualTo("ready");
            assertThat(dto.chunks()).isEqualTo(12);
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.createdAt()).isEqualTo(NOW);

            doc.setId(null);
            doc.setKnowledgeId(null);
            AiDocumentDto nullIds = documentMapper.toDto(doc);
            assertThat(nullIds.id()).isNull();
            assertThat(nullIds.knowledgeId()).isNull();
        }

        @Test
        @DisplayName("update(null, 文档) 不改；可空字段 null 时保持原值")
        void updateBranches() {
            AiDocument doc = new AiDocument();
            doc.setName("old");
            doc.setSizeKb(1);
            doc.setStatus("pending");
            doc.setChunks(2);
            doc.setEnabled(true);

            documentMapper.update(null, doc);
            assertThat(doc.getName()).isEqualTo("old");

            documentMapper.update(new AiDocumentUpdateRequest("new", 2048, "ready", 30, false), doc);
            assertThat(doc.getName()).isEqualTo("new");
            assertThat(doc.getSizeKb()).isEqualTo(2048);
            assertThat(doc.getStatus()).isEqualTo("ready");
            assertThat(doc.getChunks()).isEqualTo(30);
            assertThat(doc.isEnabled()).isFalse();

            documentMapper.update(new AiDocumentUpdateRequest(null, 4096, null, 40, null), doc);
            assertThat(doc.getName()).isEqualTo("new");
            assertThat(doc.getSizeKb()).isEqualTo(4096);
            assertThat(doc.getStatus()).isEqualTo("ready");
            assertThat(doc.getChunks()).isEqualTo(40);
            assertThat(doc.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("AiDatasetMapper")
    class DatasetCases {

        @Test
        @DisplayName("toEntity(null) 返回 null")
        void toEntityNull() {
            assertThat(datasetMapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("toEntity enabled 显式非空与默认 true 分支")
        void toEntityBranches() {
            AiDataset d = datasetMapper.toEntity(new AiDatasetCreateRequest("ds", "desc", "manual", 100, false));
            assertThat(d.getName()).isEqualTo("ds");
            assertThat(d.getDescription()).isEqualTo("desc");
            assertThat(d.getSource()).isEqualTo("manual");
            assertThat(d.getRecords()).isEqualTo(100);
            assertThat(d.isEnabled()).isFalse();

            AiDataset def = datasetMapper.toEntity(new AiDatasetCreateRequest("ds", null, "manual", 0, null));
            assertThat(def.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("toDto(null) 返回 null；全字段与 null id 分支")
        void toDtoBranches() {
            assertThat(datasetMapper.toDto(null)).isNull();

            AiDataset d = new AiDataset();
            d.setId(2L);
            d.setName("ds");
            d.setDescription("desc");
            d.setSource("manual");
            d.setRecords(100);
            d.setEnabled(true);
            AiDatasetDto dto = datasetMapper.toDto(d);
            assertThat(dto.id()).isEqualTo("2");
            assertThat(dto.name()).isEqualTo("ds");
            assertThat(dto.description()).isEqualTo("desc");
            assertThat(dto.source()).isEqualTo("manual");
            assertThat(dto.records()).isEqualTo(100);
            assertThat(dto.enabled()).isTrue();

            d.setId(null);
            assertThat(datasetMapper.toDto(d).id()).isNull();
        }

        @Test
        @DisplayName("update(null, 数据集) 不改；可空字段 null 时保持原值")
        void updateBranches() {
            AiDataset d = new AiDataset();
            d.setName("old");
            d.setDescription("old-desc");
            d.setSource("manual");
            d.setRecords(1);
            d.setEnabled(true);

            datasetMapper.update(null, d);
            assertThat(d.getName()).isEqualTo("old");

            datasetMapper.update(new AiDatasetUpdateRequest("new", "new-desc", "imported", 200, false), d);
            assertThat(d.getName()).isEqualTo("new");
            assertThat(d.getDescription()).isEqualTo("new-desc");
            assertThat(d.getSource()).isEqualTo("imported");
            assertThat(d.getRecords()).isEqualTo(200);
            assertThat(d.isEnabled()).isFalse();

            datasetMapper.update(new AiDatasetUpdateRequest(null, null, null, 300, null), d);
            assertThat(d.getName()).isEqualTo("new");
            assertThat(d.getSource()).isEqualTo("imported");
            assertThat(d.getRecords()).isEqualTo(300);
            assertThat(d.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("AiVectorStoreMapper")
    class VectorStoreCases {

        @Test
        @DisplayName("toEntity(null) 返回 null")
        void toEntityNull() {
            assertThat(vectorStoreMapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("toEntity enabled 显式非空与默认 true 分支")
        void toEntityBranches() {
            AiVectorStore v = vectorStoreMapper.toEntity(
                    new AiVectorStoreCreateRequest("pg", "pgvector", "localhost", 1024, false));
            assertThat(v.getName()).isEqualTo("pg");
            assertThat(v.getType()).isEqualTo("pgvector");
            assertThat(v.getHost()).isEqualTo("localhost");
            assertThat(v.getDimensions()).isEqualTo(1024);
            assertThat(v.isEnabled()).isFalse();

            AiVectorStore def =
                    vectorStoreMapper.toEntity(new AiVectorStoreCreateRequest("pg", "pgvector", null, 0, null));
            assertThat(def.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("toDto(null) 返回 null；全字段与 null id 分支")
        void toDtoBranches() {
            assertThat(vectorStoreMapper.toDto(null)).isNull();

            AiVectorStore v = new AiVectorStore();
            v.setId(4L);
            v.setName("pg");
            v.setType("pgvector");
            v.setHost("localhost");
            v.setDimensions(1024);
            v.setEnabled(true);
            AiVectorStoreDto dto = vectorStoreMapper.toDto(v);
            assertThat(dto.id()).isEqualTo("4");
            assertThat(dto.name()).isEqualTo("pg");
            assertThat(dto.type()).isEqualTo("pgvector");
            assertThat(dto.host()).isEqualTo("localhost");
            assertThat(dto.dimensions()).isEqualTo(1024);
            assertThat(dto.enabled()).isTrue();

            v.setId(null);
            assertThat(vectorStoreMapper.toDto(v).id()).isNull();
        }

        @Test
        @DisplayName("update(null, 向量库) 不改；可空字段 null 时保持原值")
        void updateBranches() {
            AiVectorStore v = new AiVectorStore();
            v.setName("old");
            v.setType("old-type");
            v.setHost("old-host");
            v.setDimensions(1);
            v.setEnabled(true);

            vectorStoreMapper.update(null, v);
            assertThat(v.getName()).isEqualTo("old");

            vectorStoreMapper.update(new AiVectorStoreUpdateRequest("new", "new-type", "new-host", 2048, false), v);
            assertThat(v.getName()).isEqualTo("new");
            assertThat(v.getType()).isEqualTo("new-type");
            assertThat(v.getHost()).isEqualTo("new-host");
            assertThat(v.getDimensions()).isEqualTo(2048);
            assertThat(v.isEnabled()).isFalse();

            vectorStoreMapper.update(new AiVectorStoreUpdateRequest(null, null, null, 4096, null), v);
            assertThat(v.getName()).isEqualTo("new");
            assertThat(v.getType()).isEqualTo("new-type");
            assertThat(v.getDimensions()).isEqualTo(4096);
            assertThat(v.isEnabled()).isFalse();
        }
    }
}
