// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.seed;

import dev.strata.ai.api.seed.AiSeedFile;
import dev.strata.ai.chat.api.entity.AiChatMessage;
import dev.strata.ai.chat.api.entity.AiChatSession;
import dev.strata.ai.chat.biz.repository.AiChatMessageRepository;
import dev.strata.ai.chat.biz.repository.AiChatSessionRepository;
import dev.strata.ai.config.api.entity.AiAgent;
import dev.strata.ai.config.api.entity.AiMcpServer;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.api.entity.AiPrompt;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.ai.config.api.entity.AiSkill;
import dev.strata.ai.config.api.entity.AiTerm;
import dev.strata.ai.config.biz.repository.AiAgentRepository;
import dev.strata.ai.config.biz.repository.AiMcpServerRepository;
import dev.strata.ai.config.biz.repository.AiModelRepository;
import dev.strata.ai.config.biz.repository.AiPromptRepository;
import dev.strata.ai.config.biz.repository.AiProviderRepository;
import dev.strata.ai.config.biz.repository.AiSkillRepository;
import dev.strata.ai.config.biz.repository.AiTermRepository;
import dev.strata.ai.knowledge.api.entity.AiDataset;
import dev.strata.ai.knowledge.api.entity.AiDocument;
import dev.strata.ai.knowledge.api.entity.AiKnowledge;
import dev.strata.ai.knowledge.api.entity.AiVectorStore;
import dev.strata.ai.knowledge.biz.repository.AiDatasetRepository;
import dev.strata.ai.knowledge.biz.repository.AiDocumentRepository;
import dev.strata.ai.knowledge.biz.repository.AiKnowledgeRepository;
import dev.strata.ai.knowledge.biz.repository.AiVectorStoreRepository;
import dev.strata.system.api.user.UserLookup;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(2)
@ConditionalOnProperty(name = "strata.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class AiSeedDataInitializer implements ApplicationRunner {

    private final AiProviderRepository aiProviderRepository;
    private final AiModelRepository aiModelRepository;
    private final AiPromptRepository aiPromptRepository;
    private final AiTermRepository aiTermRepository;
    private final AiVectorStoreRepository aiVectorStoreRepository;
    private final AiKnowledgeRepository aiKnowledgeRepository;
    private final AiDocumentRepository aiDocumentRepository;
    private final AiSkillRepository aiSkillRepository;
    private final AiMcpServerRepository aiMcpServerRepository;
    private final AiAgentRepository aiAgentRepository;
    private final AiDatasetRepository aiDatasetRepository;
    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final UserLookup userLookup;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    public AiSeedDataInitializer(
            AiProviderRepository aiProviderRepository,
            AiModelRepository aiModelRepository,
            AiPromptRepository aiPromptRepository,
            AiTermRepository aiTermRepository,
            AiVectorStoreRepository aiVectorStoreRepository,
            AiKnowledgeRepository aiKnowledgeRepository,
            AiDocumentRepository aiDocumentRepository,
            AiSkillRepository aiSkillRepository,
            AiMcpServerRepository aiMcpServerRepository,
            AiAgentRepository aiAgentRepository,
            AiDatasetRepository aiDatasetRepository,
            AiChatSessionRepository aiChatSessionRepository,
            AiChatMessageRepository aiChatMessageRepository,
            UserLookup userLookup,
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader) {
        this.aiProviderRepository = aiProviderRepository;
        this.aiModelRepository = aiModelRepository;
        this.aiPromptRepository = aiPromptRepository;
        this.aiTermRepository = aiTermRepository;
        this.aiVectorStoreRepository = aiVectorStoreRepository;
        this.aiKnowledgeRepository = aiKnowledgeRepository;
        this.aiDocumentRepository = aiDocumentRepository;
        this.aiSkillRepository = aiSkillRepository;
        this.aiMcpServerRepository = aiMcpServerRepository;
        this.aiAgentRepository = aiAgentRepository;
        this.aiDatasetRepository = aiDatasetRepository;
        this.aiChatSessionRepository = aiChatSessionRepository;
        this.aiChatMessageRepository = aiChatMessageRepository;
        this.userLookup = userLookup;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AiSeedFile seed = loadSeed();
        seedProviders(seed);
        seedModels(seed);
        seedPrompts(seed);
        seedTerms(seed);
        seedVectorStores(seed);
        seedKnowledge(seed);
        seedDocuments(seed);
        seedSkills(seed);
        seedMcpServers(seed);
        seedAgents(seed);
        seedDatasets(seed);
        seedChatSessions(seed);
        seedChatMessages(seed);
    }

    private AiSeedFile loadSeed() {
        try (var in =
                resourceLoader.getResource("classpath:seed/ai-seed-data.json").getInputStream()) {
            return objectMapper.readValue(in, AiSeedFile.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load AI seed data", ex);
        }
    }

    private void seedProviders(AiSeedFile seed) {
        if (aiProviderRepository.count() > 0) {
            return;
        }
        for (AiSeedFile.ProviderSeed row : seed.providers()) {
            if (aiProviderRepository.existsByCode(row.code())) {
                continue;
            }
            AiProvider provider = new AiProvider();
            provider.setCode(row.code());
            provider.setName(row.name());
            provider.setType(row.type());
            provider.setBaseUrl(row.baseUrl());
            provider.setEnabled(true);
            aiProviderRepository.save(provider);
        }
    }

    private void seedModels(AiSeedFile seed) {
        if (aiModelRepository.count() > 0) {
            return;
        }
        Map<String, Long> providerIds = providerIdsByCode();
        for (AiSeedFile.ModelSeed row : seed.models()) {
            Long providerId = providerIds.get(row.providerCode());
            if (providerId == null || aiModelRepository.existsByProviderIdAndCode(providerId, row.code())) {
                continue;
            }
            AiModel model = new AiModel();
            model.setProviderId(providerId);
            model.setCode(row.code());
            model.setName(row.name());
            model.setType(row.type());
            model.setEnabled(true);
            aiModelRepository.save(model);
        }
    }

    private void seedPrompts(AiSeedFile seed) {
        // Per-code idempotent: Flyway may have pre-seeded some prompts (e.g. chat_system) before this runner
        // fires; do not short-circuit on count>0 or those JSON templates would never land.
        for (AiSeedFile.PromptSeed row : seed.prompts()) {
            if (aiPromptRepository.existsByCode(row.code())) {
                continue;
            }
            AiPrompt prompt = new AiPrompt();
            prompt.setCode(row.code());
            prompt.setName(row.name());
            prompt.setTemplate(row.template());
            prompt.setEnabled(true);
            aiPromptRepository.save(prompt);
        }
    }

    private void seedTerms(AiSeedFile seed) {
        if (aiTermRepository.count() > 0) {
            return;
        }
        for (AiSeedFile.TermSeed row : seed.terms()) {
            AiTerm term = new AiTerm();
            term.setTerm(row.term());
            term.setTranslation(row.translation());
            term.setNotes(row.notes());
            term.setEnabled(true);
            aiTermRepository.save(term);
        }
    }

    private void seedVectorStores(AiSeedFile seed) {
        if (aiVectorStoreRepository.count() > 0) {
            return;
        }
        for (AiSeedFile.VectorStoreSeed row : seed.vectorStores()) {
            AiVectorStore store = new AiVectorStore();
            store.setName(row.name());
            store.setType(row.type());
            store.setHost(row.host());
            store.setDimensions(row.dimensions());
            store.setEnabled(true);
            aiVectorStoreRepository.save(store);
        }
    }

    private void seedKnowledge(AiSeedFile seed) {
        if (aiKnowledgeRepository.count() > 0) {
            return;
        }
        for (AiSeedFile.KnowledgeSeed row : seed.knowledge()) {
            AiKnowledge kb = new AiKnowledge();
            kb.setName(row.name());
            kb.setDescription(row.description());
            kb.setEmbeddingModel(row.embeddingModel());
            kb.setVectorStore(row.vectorStore());
            kb.setChunkSize(row.chunkSize());
            kb.setChunkOverlap(row.chunkOverlap());
            kb.setEnabled(true);
            aiKnowledgeRepository.save(kb);
        }
    }

    private void seedDocuments(AiSeedFile seed) {
        if (aiDocumentRepository.count() > 0) {
            return;
        }
        Map<String, Long> knowledgeIds = knowledgeIdsByName();
        for (AiSeedFile.DocumentSeed row : seed.documents()) {
            Long knowledgeId = knowledgeIds.get(row.knowledgeName());
            if (knowledgeId == null) {
                continue;
            }
            AiDocument doc = new AiDocument();
            doc.setKnowledgeId(knowledgeId);
            doc.setName(row.name());
            doc.setSizeKb(row.sizeKb());
            doc.setStatus(row.status());
            doc.setChunks(row.chunks());
            doc.setEnabled(true);
            aiDocumentRepository.save(doc);
        }
    }

    private void seedSkills(AiSeedFile seed) {
        if (aiSkillRepository.count() > 0) {
            return;
        }
        for (AiSeedFile.SkillSeed row : seed.skills()) {
            AiSkill skill = new AiSkill();
            skill.setName(row.name());
            skill.setType(row.type());
            skill.setPath(row.path());
            skill.setDescription(row.description());
            skill.setEnabled(true);
            aiSkillRepository.save(skill);
        }
    }

    private void seedMcpServers(AiSeedFile seed) {
        if (aiMcpServerRepository.count() > 0) {
            return;
        }
        for (AiSeedFile.McpServerSeed row : seed.mcpServers()) {
            AiMcpServer mcp = new AiMcpServer();
            mcp.setName(row.name());
            mcp.setUrl(row.url());
            mcp.setAuthType(row.authType());
            mcp.setEnabled(true);
            aiMcpServerRepository.save(mcp);
        }
    }

    private void seedAgents(AiSeedFile seed) {
        if (aiAgentRepository.count() > 0) {
            return;
        }
        for (AiSeedFile.AgentSeed row : seed.agents()) {
            AiAgent agent = new AiAgent();
            agent.setName(row.name());
            agent.setModel(row.model());
            agent.setPrompt(row.prompt());
            agent.setEnabled(true);
            aiAgentRepository.save(agent);
        }
    }

    private void seedDatasets(AiSeedFile seed) {
        if (aiDatasetRepository.count() > 0) {
            return;
        }
        for (AiSeedFile.DatasetSeed row : seed.datasets()) {
            AiDataset dataset = new AiDataset();
            dataset.setName(row.name());
            dataset.setDescription(row.description());
            dataset.setSource(row.source());
            dataset.setRecords(row.records());
            dataset.setEnabled(true);
            aiDatasetRepository.save(dataset);
        }
    }

    private void seedChatSessions(AiSeedFile seed) {
        Map<String, Long> modelIds = new HashMap<>();
        for (AiModel model : aiModelRepository.findAll()) {
            modelIds.put(model.getCode(), model.getId());
        }
        for (AiSeedFile.ChatSessionSeed row : seed.chatSessions()) {
            userLookup.findByUsername(row.username()).ifPresent(user -> {
                boolean exists = aiChatSessionRepository.findByUserIdOrderByCreatedAtDesc(user.id()).stream()
                        .anyMatch(session -> session.getTitle().equals(row.title()));
                if (exists) {
                    return;
                }
                AiChatSession session = new AiChatSession();
                session.setUserId(user.id());
                session.setTitle(row.title());
                session.setModelId(modelIds.get(row.modelCode()));
                aiChatSessionRepository.save(session);
            });
        }
    }

    private void seedChatMessages(AiSeedFile seed) {
        if (aiChatMessageRepository.count() > 0) {
            return;
        }
        Map<String, Long> sessionIds = new HashMap<>();
        for (AiChatSession session : aiChatSessionRepository.findAll()) {
            sessionIds.put(session.getTitle(), session.getId());
        }
        for (AiSeedFile.ChatMessageSeed row : seed.chatMessages()) {
            Long sessionId = sessionIds.get(row.sessionTitle());
            if (sessionId == null) {
                continue;
            }
            AiChatMessage message = new AiChatMessage();
            message.setSessionId(sessionId);
            message.setRole(row.role());
            message.setContent(row.content());
            aiChatMessageRepository.save(message);
        }
    }

    private Map<String, Long> providerIdsByCode() {
        Map<String, Long> ids = new HashMap<>();
        for (AiProvider provider : aiProviderRepository.findAll()) {
            ids.put(provider.getCode(), provider.getId());
        }
        return ids;
    }

    private Map<String, Long> knowledgeIdsByName() {
        Map<String, Long> ids = new HashMap<>();
        for (AiKnowledge knowledge : aiKnowledgeRepository.findAll()) {
            ids.put(knowledge.getName(), knowledge.getId());
        }
        return ids;
    }
}
