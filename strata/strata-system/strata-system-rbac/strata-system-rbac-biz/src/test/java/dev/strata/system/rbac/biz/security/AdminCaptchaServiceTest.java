// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AdminCaptchaService 验证码发放与校验")
class AdminCaptchaServiceTest {

    private AdminCaptchaService captchas;

    @BeforeEach
    void setUp() {
        captchas = new AdminCaptchaService();
    }

    @Test
    @DisplayName("发放后用正确答案校验通过（加法与减法）")
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
    @DisplayName("减法结果恒为非负")
    void subtraction_never_goes_negative() {
        for (int i = 0; i < 100; i++) {
            dev.strata.system.api.dto.CaptchaIssueResponse issue = captchas.issue();
            if (issue.question().contains("-")) {
                assertThat(parseAnswer(issue.question())).isGreaterThanOrEqualTo(0);
            }
        }
    }

    @Test
    @DisplayName("错误答案返回 INVALID")
    void wrong_answer_is_invalid() {
        dev.strata.system.api.dto.CaptchaIssueResponse issue = captchas.issue();
        int answer = parseAnswer(issue.question());

        assertThat(captchas.verify(issue.captchaId(), Integer.toString(answer + 1)))
                .isEqualTo(AdminCaptchaOutcome.INVALID);
    }

    @Test
    @DisplayName("缺少 id 或答案返回 MISSING")
    void missing_fields_are_missing() {
        dev.strata.system.api.dto.CaptchaIssueResponse issue = captchas.issue();

        assertThat(captchas.verify(null, "1")).isEqualTo(AdminCaptchaOutcome.MISSING);
        assertThat(captchas.verify(issue.captchaId(), "")).isEqualTo(AdminCaptchaOutcome.MISSING);
    }

    @Test
    @DisplayName("一次性：用过的 id 再次校验返回 EXPIRED")
    void consumed_id_cannot_be_reused() {
        dev.strata.system.api.dto.CaptchaIssueResponse issue = captchas.issue();
        int answer = parseAnswer(issue.question());

        assertThat(captchas.verify(issue.captchaId(), Integer.toString(answer))).isEqualTo(AdminCaptchaOutcome.OK);
        // A correct answer on a once-consumed id must fail — the id is gone from the store.
        assertThat(captchas.verify(issue.captchaId(), Integer.toString(answer))).isEqualTo(AdminCaptchaOutcome.EXPIRED);
    }

    @Test
    @DisplayName("错误答案也消耗 id：再次校验返回 EXPIRED（不泄露是否存在过）")
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
    @DisplayName("答案前后空格被容忍")
    void surrounding_whitespace_is_trimmed() {
        dev.strata.system.api.dto.CaptchaIssueResponse issue = captchas.issue();
        int answer = parseAnswer(issue.question());

        assertThat(captchas.verify(issue.captchaId(), "  " + answer + "  ")).isEqualTo(AdminCaptchaOutcome.OK);
    }

    @Test
    @DisplayName("登录触发：同一 IP 失败 3 次后要求验证码")
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
    @DisplayName("登录成功后清除失败计数")
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
    @DisplayName("不同 IP 互不影响；TTL 常量为 300 秒")
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
