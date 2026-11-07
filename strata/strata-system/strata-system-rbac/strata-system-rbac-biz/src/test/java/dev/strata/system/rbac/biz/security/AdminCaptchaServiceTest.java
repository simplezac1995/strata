// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminCaptchaServiceTest {

    private AdminCaptchaService captchas;

    @BeforeEach
    void setUp() {
        captchas = new AdminCaptchaService();
    }

    @Test
    void correct_answer_verifies() {
        boolean sawPlus = false;
        boolean sawMinus = false;
        // Re-issue until both operators are exercised: the happy path must pass for either shape.
        for (int i = 0; i < 50 && (!sawPlus || !sawMinus); i++) {
            dev.strata.system.api.dto.CaptchaIssueResponse issue = captchas.issue();
            int answer = parseAnswer(issue.question());
            assertThat(answer).isGreaterThanOrEqualTo(0);

            assertThat(captchas.verify(issue.captchaId(), Integer.toString(answer)))
                    .isEqualTo(AdminCaptchaOutcome.OK);
            if (issue.question().contains("+")) {
                sawPlus = true;
            } else {
                sawMinus = true;
            }
        }
        assertThat(sawPlus).as("covered + branch").isTrue();
        assertThat(sawMinus).as("covered - branch").isTrue();
    }

    @Test
    void subtraction_never_goes_negative() {
        for (int i = 0; i < 100; i++) {
            dev.strata.system.api.dto.CaptchaIssueResponse issue = captchas.issue();
            if (issue.question().contains("-")) {
                assertThat(parseAnswer(issue.question())).isGreaterThanOrEqualTo(0);
            }
        }
    }

    @Test
    void wrong_answer_is_invalid() {
        dev.strata.system.api.dto.CaptchaIssueResponse issue = captchas.issue();
        int answer = parseAnswer(issue.question());

        assertThat(captchas.verify(issue.captchaId(), Integer.toString(answer + 1)))
                .isEqualTo(AdminCaptchaOutcome.INVALID);
    }

    @Test
    void missing_fields_are_missing() {
        dev.strata.system.api.dto.CaptchaIssueResponse issue = captchas.issue();

        assertThat(captchas.verify(null, "1")).isEqualTo(AdminCaptchaOutcome.MISSING);
        assertThat(captchas.verify(issue.captchaId(), "")).isEqualTo(AdminCaptchaOutcome.MISSING);
    }

    @Test
    void consumed_id_cannot_be_reused() {
        dev.strata.system.api.dto.CaptchaIssueResponse issue = captchas.issue();
        int answer = parseAnswer(issue.question());

        assertThat(captchas.verify(issue.captchaId(), Integer.toString(answer))).isEqualTo(AdminCaptchaOutcome.OK);
        // A correct answer on a once-consumed id must fail — the id is gone from the store.
        assertThat(captchas.verify(issue.captchaId(), Integer.toString(answer))).isEqualTo(AdminCaptchaOutcome.EXPIRED);
    }

    @Test
    void wrong_answer_consumes_id_without_oracle() {
        dev.strata.system.api.dto.CaptchaIssueResponse issue = captchas.issue();
        int answer = parseAnswer(issue.question());

        assertThat(captchas.verify(issue.captchaId(), Integer.toString(answer + 1)))
                .isEqualTo(AdminCaptchaOutcome.INVALID);
        // Neither a retry nor an unknown id distinguishes "used/unknown/expired" — all collapse to EXPIRED.
        assertThat(captchas.verify(issue.captchaId(), Integer.toString(answer))).isEqualTo(AdminCaptchaOutcome.EXPIRED);
        assertThat(captchas.verify("does-not-exist", "1")).isEqualTo(AdminCaptchaOutcome.EXPIRED);
    }

    @Test
    void surrounding_whitespace_is_trimmed() {
        dev.strata.system.api.dto.CaptchaIssueResponse issue = captchas.issue();
        int answer = parseAnswer(issue.question());

        assertThat(captchas.verify(issue.captchaId(), "  " + answer + "  ")).isEqualTo(AdminCaptchaOutcome.OK);
    }

    @Test
    void captcha_required_after_three_failures() {
        String ip = "203.0.113.9";

        assertThat(captchas.requiresCaptcha(ip)).isFalse();
        captchas.recordFailure(ip);
        assertThat(captchas.requiresCaptcha(ip)).isFalse();
        captchas.recordFailure(ip);
        assertThat(captchas.requiresCaptcha(ip)).isFalse();
        captchas.recordFailure(ip);

        assertThat(captchas.requiresCaptcha(ip)).isTrue();
    }

    @Test
    void successful_login_clears_failures() {
        String ip = "203.0.113.10";
        captchas.recordFailure(ip);
        captchas.recordFailure(ip);
        captchas.recordFailure(ip);
        assertThat(captchas.requiresCaptcha(ip)).isTrue();

        captchas.clear(ip);
        assertThat(captchas.requiresCaptcha(ip)).isFalse();
    }

    @Test
    void isolation_and_ttl_constant() {
        String ip = "198.51.100.1" + System.nanoTime();
        assertThat(captchas.requiresCaptcha(ip)).isFalse();
        captchas.recordFailure(ip);
        captchas.recordFailure(ip);
        // A fresh IP must have no residual count.
        assertThat(captchas.requiresCaptcha("198.51.100.2" + System.nanoTime())).isFalse();
        assertThat(Duration.ofSeconds(300)).isEqualTo(AdminCaptchaService.TTL);
        assertThat(AdminCaptchaService.WINDOW.toMinutes()).isEqualTo(5);
    }

    /** Parses the answer from a question shaped {@code "a + b = ?"} or {@code "a - b = ?"}. */
    private static int parseAnswer(String question) {
        String trimmed = question.replace("= ?", "").trim();
        if (trimmed.contains("+")) {
            String[] parts = trimmed.split("\\+");
            return Integer.parseInt(parts[0].trim()) + Integer.parseInt(parts[1].trim());
        }
        String[] parts = trimmed.split("-");
        return Integer.parseInt(parts[0].trim()) - Integer.parseInt(parts[1].trim());
    }
}
