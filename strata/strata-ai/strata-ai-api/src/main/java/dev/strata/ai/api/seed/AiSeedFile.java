// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.seed;

import java.util.List;

public record AiSeedFile(
        List<ProviderSeed> providers,
        List<ModelSeed> models,
        List<PromptSeed> prompts,
        List<TermSeed> terms,
        List<VectorStoreSeed> vectorStores,
        List<KnowledgeSeed> knowledge,
        List<DocumentSeed> documents,
        List<SkillSeed> skills,
        List<McpServerSeed> mcpServers,
        List<AgentSeed> agents,
        List<DatasetSeed> datasets,
        List<ChatSessionSeed> chatSessions,
        List<ChatMessageSeed> chatMessages) {

    public record ProviderSeed(String code, String name, String type, String baseUrl) {}

    public record ModelSeed(String providerCode, String code, String name, String type) {}

    public record PromptSeed(String code, String name, String template) {}

    public record TermSeed(String term, String translation, String notes) {}

    public record VectorStoreSeed(String name, String type, String host, int dimensions) {}

    public record KnowledgeSeed(
            String name,
            String description,
            String embeddingModel,
            String vectorStore,
            int chunkSize,
            int chunkOverlap) {}

    public record DocumentSeed(String knowledgeName, String name, int sizeKb, String status, int chunks) {}

    public record SkillSeed(String name, String type, String path, String description) {}

    public record McpServerSeed(String name, String url, String authType) {}

    public record AgentSeed(String name, String model, String prompt) {}

    public record DatasetSeed(String name, String description, String source, int records) {}

    public record ChatSessionSeed(String username, String title, String modelCode) {}

    public record ChatMessageSeed(String sessionTitle, String role, String content) {}
}
