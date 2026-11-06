// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.event;

import dev.strata.ai.biz.entity.AiLinkageRule;
import dev.strata.ai.biz.repository.AiLinkageRuleRepository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Evaluates enabled {@link AiLinkageRule} rows when a recognized business event fires.
 *
 * <p>Maps a Spring {@code ApplicationEvent} to an (object, event) key by class name, then runs every
 * enabled rule that matches. Actions:
 *
 * <ul>
 *   <li>{@code notify} — sends a Telegram message to the configured chat (falls back to log if unset).
 *   <li>{@code tag} — logs (reserved for future player tagging).
 *   <li>{@code email} — logs (reserved for future mail sending).
 * </ul>
 */
@Component
public class LinkageRuleEvaluator {

    private static final Logger log = LoggerFactory.getLogger(LinkageRuleEvaluator.class);

    /** Maps event class simple-name → (object, event) pair used in {@code ai_linkage_rule}. */
    private static final Map<String, String[]> EVENT_MAP = Map.ofEntries(
            Map.entry("WalletBalanceChangedEvent", new String[] {"player", "balance_changed"}),
            Map.entry("ChainAlertEvent", new String[] {"wallet", "chain_alert"}),
            Map.entry("LoginLogEvent", new String[] {"user", "login"}));

    private final AiLinkageRuleRepository repository;
    private final AtomicReference<RestClient> http = new AtomicReference<>();

    @Value("${strata.telegram.bot-token:}")
    private String botToken;

    @Value("${strata.telegram.chat-id:}")
    private String chatId;

    public LinkageRuleEvaluator(AiLinkageRuleRepository repository) {
        this.repository = repository;
    }

    @Async
    @EventListener
    public void onBusinessEvent(Object event) {
        String key = event.getClass().getSimpleName();
        String[] mapping = EVENT_MAP.get(key);
        if (mapping == null) {
            return;
        }
        String object = mapping[0];
        String eventName = mapping[1];

        List<AiLinkageRule> rules = repository.findByEnabledTrueAndObjectAndEvent(object, eventName);
        for (AiLinkageRule rule : rules) {
            execute(rule, event);
        }
    }

    private void execute(AiLinkageRule rule, Object event) {
        try {
            switch (rule.getAction() == null ? "" : rule.getAction()) {
                case "notify" -> sendTelegram(rule, event);
                case "tag" ->
                    log.info("[AI-linkage] rule='{}' action=tag (reserved) payload={}", rule.getName(), event);
                case "email" ->
                    log.info("[AI-linkage] rule='{}' action=email (reserved) payload={}", rule.getName(), event);
                default -> log.warn("[AI-linkage] rule='{}' unknown action='{}'", rule.getName(), rule.getAction());
            }
        } catch (Exception ex) {
            log.error("[AI-linkage] rule='{}' failed", rule.getName(), ex);
        }
    }

    private void sendTelegram(AiLinkageRule rule, Object event) {
        String text = "[AI联动] " + rule.getName() + "\n对象: " + rule.getObject()
                + "\n事件: " + rule.getEvent()
                + "\n详情: " + event;
        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            log.info("[AI-linkage] notify (no telegram configured): {}", text);
            return;
        }
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        Map<String, Object> body = Map.of("chat_id", chatId, "text", text);
        httpClient()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        log.info("[AI-linkage] notify sent to telegram for rule='{}'", rule.getName());
    }

    private RestClient httpClient() {
        RestClient client = http.get();
        if (client != null) {
            return client;
        }
        synchronized (http) {
            client = http.get();
            if (client == null) {
                client = RestClient.create();
                http.set(client);
            }
        }
        return client;
    }
}
