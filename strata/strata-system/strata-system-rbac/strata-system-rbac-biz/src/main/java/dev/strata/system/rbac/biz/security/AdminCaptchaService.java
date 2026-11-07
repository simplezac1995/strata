// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.security;

import dev.strata.system.api.dto.CaptchaIssueResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Lightweight math captcha for the admin login surface, plus the per-IP abuse trigger that decides when the
 * captcha is demanded.
 *
 * <p>Mirrors the casino-side {@code CaptchaService} (same threat model: blunt credential stuffing without
 * burdening the ordinary admin) but keeps the in-memory store local to the system-rbac module:
 *
 * <ul>
 *   <li><b>Challenges.</b> A short arithmetic question ({@code a + b} or {@code a - b}, operands 1..20, result
 *       always non-negative) with a 300-second TTL. The answer lives in a {@link ConcurrentHashMap} keyed by a
 *       random hex id. Verification is one-shot: the id is removed on every check, so a stolen or replayed answer
 *       fails as {@link AdminCaptchaOutcome#EXPIRED} — the same verdict whether the id was used, unknown or
 *       expired, so there is no oracle for "did this captcha exist".
 *   <li><b>Trigger.</b> A captcha is asked for only after 3 failed logins from one IP inside a sliding 5-minute
 *       window, so the happy path (below threshold) is untouched and the counter resets on a successful login.
 * </ul>
 */
@Service
public class AdminCaptchaService {

    /** How long an issued captcha stays answerable. */
    static final Duration TTL = Duration.ofSeconds(300);

    /** The sliding window over which login failures are counted per IP. */
    static final Duration WINDOW = Duration.ofMinutes(5);

    /** A captcha is demanded after this many failed logins from one IP inside {@link #WINDOW}. */
    static final int FAIL_THRESHOLD = 3;

    private static final int QUESTION_MAX = 20;
    private static final int ID_BYTES = 16;

    private final SecureRandom random = new SecureRandom();

    /** Master switch for captcha enforcement (prod on; integration tests flip off). */
    @Value("${strata.system.admin-captcha.enabled:true}")
    private boolean enabled = true;

    /** @return whether captcha challenges / triggers are active at all. */
    public boolean isEnabled() {
        return enabled;
    }

    /** captchaId -> (answer, expiry). */
    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    /** IP -> epoch-millis login failures inside the window. */
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> loginFails = new ConcurrentHashMap<>();

    private record Entry(String answer, Instant expiresAt) {}

    /** Mints a new arithmetic challenge, stores its answer, and hands it back to the controller. */
    public CaptchaIssueResponse issue() {
        int a = 1 + random.nextInt(QUESTION_MAX);
        int b = 1 + random.nextInt(QUESTION_MAX);
        int answer;
        String question;
        if (random.nextBoolean()) {
            // Subtraction: keep the result non-negative by making the larger operand the minuend.
            if (a < b) {
                int swap = a;
                a = b;
                b = swap;
            }
            answer = a - b;
            question = a + " - " + b + " = ?";
        } else {
            answer = a + b;
            question = a + " + " + b + " = ?";
        }
        String id = HexFormat.of().formatHex(generate(ID_BYTES));
        store.put(id, new Entry(Integer.toString(answer), Instant.now().plus(TTL)));
        return new CaptchaIssueResponse(id, question);
    }

    /**
     * Verifies and consumes a captcha. The id is removed on every call (one-time use), so replaying a once-good
     * answer fails.
     *
     * <p>{@link AdminCaptchaOutcome#MISSING} is returned when either argument is blank, so a caller that only
     * demands a captcha above a threshold can tell "not sent" apart from "sent and wrong".
     */
    public AdminCaptchaOutcome verify(String captchaId, String answer) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(answer)) {
            return AdminCaptchaOutcome.MISSING;
        }
        Entry entry = store.remove(captchaId);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return AdminCaptchaOutcome.EXPIRED;
        }
        if (MessageDigest.isEqual(
                entry.answer().getBytes(StandardCharsets.UTF_8), answer.trim().getBytes(StandardCharsets.UTF_8))) {
            return AdminCaptchaOutcome.OK;
        }
        return AdminCaptchaOutcome.INVALID;
    }

    // --- per-IP sliding window ---

    /** @return whether the IP has crossed the failed-login threshold and must pass a captcha before next login. */
    public boolean requiresCaptcha(String ip) {
        return enabled && countInWindow(ip) >= FAIL_THRESHOLD;
    }

    /** Records a failed login attempt from {@code ip}; call on an {@code INVALID_CREDENTIALS} result. */
    public void recordFailure(String ip) {
        long now = Instant.now().toEpochMilli();
        long cutoff = now - WINDOW.toMillis();
        ConcurrentLinkedDeque<Long> deque =
                loginFails.computeIfAbsent(normalize(ip), k -> new ConcurrentLinkedDeque<>());
        deque.removeIf(ts -> ts < cutoff);
        deque.addLast(now);
    }

    /** Drops the failure count for an IP that just authenticated successfully. */
    public void clear(String ip) {
        loginFails.remove(normalize(ip));
    }

    private int countInWindow(String ip) {
        ConcurrentLinkedDeque<Long> deque = loginFails.get(normalize(ip));
        if (deque == null) {
            return 0;
        }
        long cutoff = Instant.now().toEpochMilli() - WINDOW.toMillis();
        deque.removeIf(ts -> ts < cutoff);
        return deque.size();
    }

    private static String normalize(String ip) {
        return StringUtils.hasText(ip) ? ip : "unknown";
    }

    private byte[] generate(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }
}
