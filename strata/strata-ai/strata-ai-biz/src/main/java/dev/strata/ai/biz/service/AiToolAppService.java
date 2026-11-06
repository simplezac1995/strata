// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.service;

import dev.strata.ai.api.dto.AiToolRunDto;
import dev.strata.ai.api.dto.AiUsageDto;
import dev.strata.ai.api.request.AiToolRunRequest;
import dev.strata.ai.chat.biz.repository.AiChatMessageRepository;
import dev.strata.ai.chat.biz.repository.AiChatSessionRepository;
import dev.strata.ai.chat.biz.service.LlmGatewaySupport;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.ai.config.biz.repository.AiModelRepository;
import dev.strata.ai.config.biz.repository.AiProviderRepository;
import dev.strata.ai.knowledge.biz.repository.AiDocumentRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.plugin.api.llm.LlmChatCommand;
import dev.strata.plugin.api.llm.LlmContentPart;
import dev.strata.plugin.api.llm.LlmGateway;
import dev.strata.system.api.support.SensitiveWordScanner;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Service
public class AiToolAppService {

    private static final Logger log = LoggerFactory.getLogger(AiToolAppService.class);

    private final LlmGateway llmGateway;
    private final AiProviderRepository aiProviderRepository;
    private final AiModelRepository aiModelRepository;
    private final SensitiveWordScanner sensitiveWordScanner;
    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final AiDocumentRepository aiDocumentRepository;

    public AiToolAppService(
            LlmGateway llmGateway,
            AiProviderRepository aiProviderRepository,
            AiModelRepository aiModelRepository,
            SensitiveWordScanner sensitiveWordScanner,
            AiChatSessionRepository aiChatSessionRepository,
            AiChatMessageRepository aiChatMessageRepository,
            AiDocumentRepository aiDocumentRepository) {
        this.llmGateway = llmGateway;
        this.aiProviderRepository = aiProviderRepository;
        this.aiModelRepository = aiModelRepository;
        this.sensitiveWordScanner = sensitiveWordScanner;
        this.aiChatSessionRepository = aiChatSessionRepository;
        this.aiChatMessageRepository = aiChatMessageRepository;
        this.aiDocumentRepository = aiDocumentRepository;
    }

    @Transactional(readOnly = true)
    public AiUsageDto usage() {
        return new AiUsageDto(
                aiChatSessionRepository.count(),
                aiChatMessageRepository.count(),
                aiProviderRepository.count(),
                aiModelRepository.count(),
                aiDocumentRepository.count());
    }

    public AiToolRunDto run(AiToolRunRequest request) {
        String tool = request.tool().trim().toLowerCase(Locale.ROOT);
        return switch (tool) {
            case "proofread", "safety", "code-review", "outline", "rewrite" -> runTextTool(tool, request.content());
            case "image" -> runImageTool(request.content());
            case "video" -> runVideoTool(request.content());
            case "tts" -> runTtsTool(request.content());
            case "ocr" -> runOcrTool(request.content());
            case "poster" -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Poster tool not configured.");
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Unknown AI tool: " + tool);
        };
    }

    private AiToolRunDto runImageTool(String content) {
        String ratio = "1:1";
        int count = 1;
        String prompt = content;
        int bracket = content.indexOf('[');
        if (bracket >= 0) {
            int close = content.indexOf(']', bracket);
            if (close > bracket) {
                String meta = content.substring(bracket + 1, close);
                prompt = content.substring(0, bracket).trim();
                for (String part : meta.split(",")) {
                    String[] kv = part.split("=");
                    if (kv.length == 2) {
                        if (kv[0].trim().equals("ratio")) ratio = kv[1].trim();
                        if (kv[0].trim().equals("count")) {
                            try {
                                count = Integer.parseInt(kv[1].trim());
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
        }
        String size =
                switch (ratio) {
                    case "3:4" -> "768*1024";
                    case "4:3" -> "1024*768";
                    case "16:9" -> "1280*720";
                    case "9:16" -> "720*1280";
                    default -> "1024*1024";
                };

        AiProvider provider = aiProviderRepository
                .findFirstByEnabledTrueOrderByIdAsc()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "No enabled AI provider configured"));
        AiProvider dashscope = aiProviderRepository.findAll().stream()
                .filter(p -> p.isEnabled() && p.getCode() != null && p.getCode().contains("dashscope"))
                .findFirst()
                .orElse(provider);
        provider = dashscope;
        String model = "wanx2.1-t2i-turbo";

        try {
            String dashscopeBase = "https://dashscope.aliyuncs.com/api/v1";
            // Text-to-image models render Chinese characters badly; steer them away from
            // drawing literal text onto the canvas. If a real poster-with-words is wanted,
            // use the poster page (DOM-overlay text).
            String imagePrompt = prompt
                    + ", no text, no letters, no characters, no words, no watermark, typography-free, high quality, detailed";
            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", Map.of("prompt", imagePrompt),
                    "parameters", Map.of("size", size, "n", count));
            JsonNode submit = restClient()
                    .post()
                    .uri(dashscopeBase + "/services/aigc/text2image/image-synthesis")
                    .header("Authorization", "Bearer " + provider.getApiKey())
                    .header("X-DashScope-Async", "enable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String taskId = submit.path("output").path("task_id").asText();
            if (taskId.isEmpty())
                throw new BusinessException(ErrorCode.BAD_REQUEST, "No task_id from DashScope: " + submit);
            List<String> urls = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                Thread.sleep(2000);
                JsonNode task = restClient()
                        .get()
                        .uri(dashscopeBase + "/tasks/" + taskId)
                        .header("Authorization", "Bearer " + provider.getApiKey())
                        .retrieve()
                        .body(JsonNode.class);
                String status = task.path("output").path("task_status").asText();
                if ("SUCCEEDED".equals(status)) {
                    JsonNode results = task.path("output").path("results");
                    if (results != null && results.isArray()) {
                        for (JsonNode r : results) {
                            if (r.has("url")) urls.add(r.get("url").asText());
                        }
                    }
                    break;
                }
                if ("FAILED".equals(status)) {
                    throw new BusinessException(
                            ErrorCode.BAD_REQUEST,
                            "Image generation failed: "
                                    + task.path("output").path("message").asText());
                }
            }
            if (urls.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, "No image returned");
            return new AiToolRunDto("image", String.join("\n", urls), true);
        } catch (BusinessException be) {
            throw be;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Image generation interrupted");
        } catch (Exception ex) {
            log.error("Image generation failed", ex);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Image generation failed: " + ex.getMessage());
        }
    }

    private AiToolRunDto runVideoTool(String content) {
        String prompt = content;
        String ratio = "16:9";
        int bracket = content.indexOf('[');
        if (bracket >= 0) {
            int close = content.indexOf(']', bracket);
            if (close > bracket) {
                String meta = content.substring(bracket + 1, close);
                prompt = content.substring(0, bracket).trim();
                for (String part : meta.split(",")) {
                    String[] kv = part.split("=");
                    if (kv.length == 2 && kv[0].trim().equals("ratio")) ratio = kv[1].trim();
                }
            }
        }
        String size =
                switch (ratio) {
                    case "9:16" -> "720*1280";
                    case "1:1" -> "720*720";
                    default -> "1280*720";
                };
        AiProvider dashscope = aiProviderRepository.findAll().stream()
                .filter(p -> p.isEnabled() && p.getCode() != null && p.getCode().contains("dashscope"))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "DashScope provider not configured"));
        try {
            String base = "https://dashscope.aliyuncs.com/api/v1";
            Map<String, Object> body = Map.of(
                    "model", "wanx2.1-t2v-turbo",
                    "input", Map.of("prompt", prompt),
                    "parameters", Map.of("size", size));
            JsonNode resp = restClient()
                    .post()
                    .uri(base + "/services/aigc/video-generation/video-synthesis")
                    .header("Authorization", "Bearer " + dashscope.getApiKey())
                    .header("X-DashScope-Async", "enable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String taskId = resp.path("output").path("task_id").asText();
            return new AiToolRunDto("video", "task_id=" + taskId, true);
        } catch (Exception ex) {
            log.error("Video generation submit failed", ex);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Video generation failed: " + ex.getMessage());
        }
    }

    public Map<String, Object> videoTaskStatus(String taskId) {
        AiProvider dashscope = aiProviderRepository.findAll().stream()
                .filter(p -> p.isEnabled() && p.getCode() != null && p.getCode().contains("dashscope"))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "DashScope provider not configured"));
        try {
            JsonNode resp = restClient()
                    .get()
                    .uri("https://dashscope.aliyuncs.com/api/v1/tasks/" + taskId)
                    .header("Authorization", "Bearer " + dashscope.getApiKey())
                    .retrieve()
                    .body(JsonNode.class);
            if (resp == null) return Map.of("status", "unknown");
            JsonNode output = resp.path("output");
            String taskStatus = output.path("task_status").asText("UNKNOWN");
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("status", taskStatus);
            if (output.has("video_url"))
                result.put("videoUrl", output.path("video_url").asText());
            if (output.has("message"))
                result.put("message", output.path("message").asText());
            return result;
        } catch (Exception ex) {
            log.error("Video status check failed", ex);
            return Map.of("status", "FAILED", "message", ex.getMessage());
        }
    }

    private AiToolRunDto runTtsTool(String content) {
        TtsParams params = parseTtsMeta(content);
        AiProvider dashscope = aiProviderRepository.findAll().stream()
                .filter(p -> p.isEnabled() && p.getCode() != null && p.getCode().contains("dashscope"))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "DashScope provider not configured"));
        try {
            // DashScope CosyVoice 非实时语音合成（HTTP 同步接口）：
            // POST /api/v1/services/audio/tts/SpeechSynthesizer
            // model=cosyvoice-v3-flash（HTTP 非流式；cosyvoice-v1 仅 WebSocket 实时，不走 HTTP），
            // 音色用 v3 命名（longxiaochun_v3 等，由前端 v1 命名映射而来），
            // 响应返回 output.audio.url（OSS 临时链接），下载后以 data URI 回给前端。
            Map<String, Object> body = Map.of(
                    "model", "cosyvoice-v3-flash",
                    "input", Map.of("text", params.text(), "voice", toV3Voice(params.voice())),
                    "parameters", Map.of("format", "mp3", "sample_rate", 24000, "speed", params.speed()));
            JsonNode resp = restClient()
                    .post()
                    .uri("https://dashscope.aliyuncs.com/api/v1/services/audio/tts/SpeechSynthesizer")
                    .header("Authorization", "Bearer " + dashscope.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (resp == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "TTS returned empty response");
            String audioUrl = resp.path("output").path("audio").path("url").asText();
            if (audioUrl.isEmpty())
                throw new BusinessException(ErrorCode.BAD_REQUEST, "No audio url from DashScope CosyVoice: " + resp);
            // 注意：OSS 预签名 URL 的 Signature 参数含 %3D 等转义，必须用 uri(URI) 原样直发，
            // uri(String) 会经 Spring URI 模板二次编码（%3D→%253D）导致 SignatureDoesNotMatch(403)。
            byte[] audio =
                    restClient().get().uri(URI.create(audioUrl)).retrieve().body(byte[].class);
            if (audio == null || audio.length == 0)
                throw new BusinessException(ErrorCode.BAD_REQUEST, "TTS audio download returned empty");
            return new AiToolRunDto(
                    "tts",
                    "data:audio/mp3;base64," + java.util.Base64.getEncoder().encodeToString(audio),
                    true);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception ex) {
            log.error("TTS failed", ex);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "TTS failed: " + ex.getMessage());
        }
    }

    /** 前端音色选项沿用 v1 命名（longxiaochun 等），映射到 cosyvoice-v3 系列音色；未知音色原样透传。包内可见供单测。 */
    static String toV3Voice(String voice) {
        return switch (voice) {
            case "longxiaochun" -> "longxiaochun_v3";
            case "longwan" -> "longwan_v3";
            case "longcheng" -> "longcheng_v3";
            case "longjielidou" -> "longjielidou_v3";
            default -> voice;
        };
    }
    /** 从 "{text}[voice=xxx, speed=n]" 解析 TTS 参数，独立出来便于单测。 */
    static TtsParams parseTtsMeta(String content) {
        String text = content;
        String voice = "longxiaochun";
        float speed = 1.0f;
        int bracket = content.lastIndexOf('[');
        if (bracket >= 0) {
            int close = content.indexOf(']', bracket);
            if (close > bracket) {
                text = content.substring(0, bracket).trim();
                String meta = content.substring(bracket + 1, close);
                for (String part : meta.split(",")) {
                    String[] kv = part.split("=");
                    if (kv.length != 2) continue;
                    String key = kv[0].trim();
                    String value = kv[1].trim();
                    if (key.equals("voice") && !value.isEmpty()) voice = value;
                    if (key.equals("speed")) {
                        try {
                            speed = Float.parseFloat(value);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }
        speed = Math.max(0.5f, Math.min(2.0f, speed));
        return new TtsParams(text, voice, speed);
    }

    record TtsParams(String text, String voice, float speed) {}

    /**
     * 外部供应商调用统一走带超时的客户端：任何挂起都必须快速失败，而不是无限占用线程与 DB 连接
     * （此前无超时导致外呼黑洞时，@Transactional 事务连接被持有 20s+ 触发 Hikari 泄漏告警）。
     */
    private static RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(60).toMillis());
        return RestClient.builder().requestFactory(factory).build();
    }

    private AiToolRunDto runOcrTool(String imageUrl) {
        AiModel vision = aiModelRepository
                .findFirstByEnabledTrueAndTypeOrderByIdAsc("vision")
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BAD_REQUEST,
                        "No enabled vision model configured; enable a vision-type model (e.g. qwen-vl-max) for OCR"));
        AiProvider provider = aiProviderRepository
                .findById(vision.getProviderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Vision model's provider not found"));
        String prompt = "Extract all text from this image. Return only the extracted text, preserve line breaks.";
        List<LlmContentPart> parts =
                List.of(new LlmContentPart("text", prompt, null), new LlmContentPart("image_url", null, imageUrl));
        LlmChatCommand command = new LlmChatCommand(
                provider.getBaseUrl(), provider.getApiKey(), vision.getCode(), List.of(), prompt, parts);
        String result = llmGateway.chat(command);
        return new AiToolRunDto("ocr", result, true);
    }

    private AiToolRunDto runTextTool(String tool, String content) {
        String localHits = sensitiveWordScanner.scanHits(content);
        try {
            ResolvedLlm llm = resolveLlm();
            String prompt = buildPrompt(tool, content);
            String llmResult =
                    llmGateway.chat(LlmGatewaySupport.toCommand(llm.provider(), llm.model(), List.of(), prompt));
            String combined = localHits.isBlank()
                    ? llmResult
                    : "## Local sensitive-word hits\n" + localHits + "\n\n## Model review\n" + llmResult;
            return new AiToolRunDto(tool, combined, true);
        } catch (BusinessException ex) {
            log.error("Error in AiToolAppService: {}", ex.getMessage(), ex);
            if (!localHits.isBlank()) {
                return new AiToolRunDto(
                        tool,
                        "## Local sensitive-word hits\n" + localHits + "\n\n(LLM unavailable: " + ex.getMessage() + ")",
                        false);
            }
            throw ex;
        }
    }

    private static String buildPrompt(String tool, String content) {
        String instruction =
                switch (tool) {
                    case "proofread" ->
                        "Proofread the following text. List issues as bullet points with severity (error/warning/info).";
                    case "safety" -> "Perform a content safety review. Score risk 0-100 and list violations if any.";
                    case "code-review" ->
                        "Review the following code or diff. List findings with severity and suggested fixes.";
                    case "outline" ->
                        "Create a concise presentation outline (title + slide bullets) for the topic below.";
                    case "rewrite" -> "Rewrite the text to be clearer and more professional. Return only the rewrite.";
                    default -> "Analyze the following content.";
                };
        return instruction + "\n\n---\n" + content;
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
