// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.service;

import dev.strata.ai.api.dto.AiOfficeResponse;
import dev.strata.ai.api.dto.AiTokenUsageDto;
import dev.strata.ai.api.request.AiOfficeRequest;
import dev.strata.ai.chat.biz.service.LlmGatewaySupport;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.api.entity.AiPrompt;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.ai.config.api.entity.AiTerm;
import dev.strata.ai.config.biz.repository.AiModelRepository;
import dev.strata.ai.config.biz.repository.AiPromptRepository;
import dev.strata.ai.config.biz.repository.AiProviderRepository;
import dev.strata.ai.config.biz.repository.AiTermRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.plugin.api.llm.LlmChatResult;
import dev.strata.plugin.api.llm.LlmGateway;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI office text transforms (generate / rewrite / translate / expand / report). System prompts are
 * read from {@code sys_ai_prompt} by code when available, falling back to the built-in defaults.
 * The translate action also appends the enabled glossary from {@code sys_ai_term} so terminology is
 * consistent across runs.
 */
@Service
public class AiOfficeAppService {

    private static final Logger log = LoggerFactory.getLogger(AiOfficeAppService.class);

    private static final Set<String> ACTIONS = Set.of("generate", "rewrite", "translate", "expand", "report");

    /** Built-in fallback prompts when sys_ai_prompt has no row for the action code. */
    private static final Map<String, String> DEFAULT_PROMPTS = Map.of(
            "generate", "You are a professional copywriter. Generate marketing copy based on: ",
            "rewrite", "Rewrite the following text to be clearer and more professional: ",
            "translate", "Translate the following text to English (or Chinese if input is English): ",
            "expand", "Expand the following text with more detail and examples: ",
            "report", "Write a structured business report based on: ");

    /** Office action code -> sys_ai_prompt.code. */
    private static final Map<String, String> PROMPT_CODE = Map.of(
            "translate", "translate-zh",
            "summarize", "summarize",
            "support-reply", "support-reply");

    private final LlmGateway llmGateway;
    private final AiProviderRepository aiProviderRepository;
    private final AiModelRepository aiModelRepository;
    private final AiUsageRecorder usageRecorder;
    private final AiPromptRepository aiPromptRepository;
    private final AiTermRepository aiTermRepository;

    public AiOfficeAppService(
            LlmGateway llmGateway,
            AiProviderRepository aiProviderRepository,
            AiModelRepository aiModelRepository,
            AiUsageRecorder usageRecorder,
            AiPromptRepository aiPromptRepository,
            AiTermRepository aiTermRepository) {
        this.llmGateway = llmGateway;
        this.aiProviderRepository = aiProviderRepository;
        this.aiModelRepository = aiModelRepository;
        this.usageRecorder = usageRecorder;
        this.aiPromptRepository = aiPromptRepository;
        this.aiTermRepository = aiTermRepository;
    }

    @Transactional(readOnly = true)
    public AiOfficeResponse run(String action, AiOfficeRequest request) {
        String key = action == null ? "" : action.toLowerCase(Locale.ROOT);
        if (!ACTIONS.contains(key)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported office action: " + action);
        }
        ResolvedLlm llm = resolveLlm();
        String systemPrompt = resolveSystemPrompt(key);
        if ("translate".equals(key)) {
            systemPrompt = systemPrompt + glossarySuffix();
        }
        String prompt = systemPrompt + request.text();
        LlmChatResult result;
        try {
            result = llmGateway.chatWithUsage(
                    LlmGatewaySupport.toCommand(llm.provider(), llm.model(), List.of(), prompt));
        } catch (RuntimeException failure) {
            log.error("AI office action {} failed", key, failure);
            throw failure;
        }
        usageRecorder.record(llm.provider().getCode(), llm.model().getCode(), null, "office", result);
        AiTokenUsageDto usage = new AiTokenUsageDto(
                result.promptTokensOrZero(), result.completionTokensOrZero(), result.totalTokensOrZero());
        return new AiOfficeResponse(result.content(), usage);
    }

    /** Read the operator-editable prompt from sys_ai_prompt, falling back to the built-in default. */
    private String resolveSystemPrompt(String action) {
        String code = PROMPT_CODE.get(action);
        if (code != null) {
            try {
                String template = aiPromptRepository
                        .findByCodeAndEnabledTrue(code)
                        .map(AiPrompt::getTemplate)
                        .filter(t -> t != null && !t.isBlank())
                        .orElse(null);
                if (template != null) {
                    return template;
                }
            } catch (Exception e) {
                log.warn("Failed to load prompt {}: {}", code, e.getMessage());
            }
        }
        return DEFAULT_PROMPTS.getOrDefault(action, "");
    }

    /** Append the enabled glossary so translations use approved terminology. */
    private String glossarySuffix() {
        try {
            List<AiTerm> terms = aiTermRepository.findAllByEnabledTrueOrderByIdAsc();
            if (terms.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder("\n\nGlossary (use these translations exactly):\n");
            for (AiTerm t : terms) {
                sb.append("- ").append(t.getTerm()).append(" = ").append(t.getTranslation());
                if (t.getNotes() != null && !t.getNotes().isBlank()) {
                    sb.append(" (").append(t.getNotes()).append(")");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Failed to load glossary: {}", e.getMessage());
            return "";
        }
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
