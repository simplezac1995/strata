// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.event;

import static org.mockito.Mockito.*;

import dev.strata.ai.biz.entity.AiLinkageRule;
import dev.strata.ai.biz.repository.AiLinkageRuleRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LinkageRuleEvaluatorTest {

    @Test
    void unknown_event_is_ignored() {
        AiLinkageRuleRepository repo = mock(AiLinkageRuleRepository.class);
        LinkageRuleEvaluator eval = new LinkageRuleEvaluator(repo);
        eval.onBusinessEvent(new Object());
        verifyNoInteractions(repo);
    }

    @Test
    void notify_rule_without_telegram_logs() {
        AiLinkageRuleRepository repo = mock(AiLinkageRuleRepository.class);
        AiLinkageRule rule = new AiLinkageRule();
        rule.setName("login-notify");
        rule.setObject("user");
        rule.setEvent("login");
        rule.setAction("notify");
        when(repo.findByEnabledTrueAndObjectAndEvent("user", "login")).thenReturn(List.of(rule));

        LinkageRuleEvaluator eval = new LinkageRuleEvaluator(repo);
        ReflectionTestUtils.setField(eval, "botToken", "");
        ReflectionTestUtils.setField(eval, "chatId", "");

        eval.onBusinessEvent(new LoginLogEvent());
        verify(repo).findByEnabledTrueAndObjectAndEvent("user", "login");
    }

    @Test
    void tag_and_email_actions_are_noops() {
        AiLinkageRuleRepository repo = mock(AiLinkageRuleRepository.class);
        AiLinkageRule tag = new AiLinkageRule();
        tag.setAction("tag");
        tag.setName("t");
        AiLinkageRule email = new AiLinkageRule();
        email.setAction("email");
        email.setName("e");
        when(repo.findByEnabledTrueAndObjectAndEvent("user", "login")).thenReturn(List.of(tag, email));

        LinkageRuleEvaluator eval = new LinkageRuleEvaluator(repo);
        eval.onBusinessEvent(new LoginLogEvent());
    }

    /** Minimal event with simple name matching EVENT_MAP. */
    static class LoginLogEvent {}
}
