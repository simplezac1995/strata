// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.frontier;

import dev.strata.system.api.request.FrontierErrorRequest;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Receives browser-side error reports (#21). Does not persist anything: it emits one structured ERROR log
 * per accepted report and applies a light in-memory per-IP fixed-window rate limit. Over-limit reports are
 * silently dropped (the caller still gets 202) so a misbehaving client cannot flood the logs.
 */
@Service
public class FrontierErrorAppService {

    private static final Logger log = LoggerFactory.getLogger(FrontierErrorAppService.class);

    /** Max accepted reports per IP per rolling 60s window. */
    static final int MAX_PER_WINDOW = 20;

    private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();

    /**
     * Fixed-window counters keyed by {@code ip:windowEpoch}. Entries age out lazily on write.
     */
    private final ConcurrentHashMap<String, Integer> counters = new ConcurrentHashMap<>();

    /**
     * @return {@code true} when the report was logged; {@code false} when the caller was rate-limited.
     *         Either way the controller answers 202.
     */
    public boolean report(FrontierErrorRequest request, String clientIp) {
        String ip = clientIp == null ? "unknown" : clientIp;
        if (!tryAcquire(ip)) {
            log.warn("Frontier error report rate-limited for ip={}", ip);
            return false;
        }
        String traceId = MDC.get("traceId");
        log.error(
                "frontier-error [traceId={}] level={} userId={} url={} message={} userAgent={} timestamp={} context={} stack={}",
                traceId,
                request == null ? null : request.level(),
                request == null ? null : request.userId(),
                request == null ? null : request.url(),
                request == null ? null : request.message(),
                request == null ? null : request.userAgent(),
                request == null ? null : request.timestamp(),
                request == null ? null : request.context(),
                request == null ? null : request.stack());
        return true;
    }

    boolean tryAcquire(String ip) {
        long window = System.currentTimeMillis() / WINDOW_MILLIS;
        String key = ip + ':' + window;
        counters.compute(key, (k, count) -> {
            int next = (count == null ? 0 : count) + 1;
            return next > MAX_PER_WINDOW ? MAX_PER_WINDOW + 1 : next;
        });
        evictOld(window);
        return counters.get(key) <= MAX_PER_WINDOW;
    }

    private void evictOld(long currentWindow) {
        counters.entrySet().removeIf(entry -> {
            int colon = entry.getKey().lastIndexOf(':');
            if (colon < 0) {
                return true;
            }
            try {
                long window = Long.parseLong(entry.getKey().substring(colon + 1));
                return window < currentWindow - 1;
            } catch (NumberFormatException ex) {
                return true;
            }
        });
    }

    /** Visible for tests. */
    Map<String, Integer> countersView() {
        return counters;
    }
}
