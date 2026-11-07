// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import dev.strata.common.core.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Runs a write exactly once per {@code Idempotency-Key} and replays the first response for every repeat.
 *
 * <p>Why a filter and not an annotation: the guarantee has to hold for *any* write endpoint, including
 * ones added later, and it must be impossible to forget. A client that retries a deposit after a timeout
 * — the normal case on a weak mobile network — has to get the original answer back instead of a second
 * deposit.
 *
 * <p>Ordering: this filter is registered as a plain servlet filter with the default lowest precedence, so
 * it runs *after* the security chain. That matters twice: the request is already authenticated (the scope
 * comes from the principal), and an unauthenticated caller cannot burn keys that belong to a real user.
 *
 * <p>What is deliberately *not* stored: failed attempts. A 5xx (or 408/429, or an exception) releases the
 * key, so the client can retry; a 2xx/4xx result is kept and replayed verbatim, because replaying is only
 * meaningful if the error the first caller saw is stable too.
 */
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);

    /** Marks a response that came out of the store rather than from the handler. */
    public static final String REPLAYED_HEADER = "Idempotency-Replayed";

    private static final PathPatternParser PATTERN_PARSER = new PathPatternParser();

    private final IdempotencyProperties properties;
    private final IdempotencyStore store;
    private final IdempotencyErrorWriter errorWriter;
    private final IdempotencyScopeResolver scopeResolver;
    private final List<PathPattern> enforcedPatterns;
    private final List<PathPattern> requiredPatterns;

    public IdempotencyFilter(
            IdempotencyProperties properties,
            IdempotencyStore store,
            IdempotencyErrorWriter errorWriter,
            IdempotencyScopeResolver scopeResolver) {
        this.properties = properties;
        this.store = store;
        this.errorWriter = errorWriter;
        this.scopeResolver = scopeResolver;
        this.enforcedPatterns = compile(properties.pathPatterns());
        this.requiredPatterns = compile(properties.requiredPathPatterns());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.enabled() || !properties.methods().contains(request.getMethod())) {
            return true;
        }
        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith("multipart/")) {
            // Uploads are streamed on purpose: buffering them to fingerprint the body would defeat the cap.
            return true;
        }
        PathContainer path = PathContainer.parsePath(pathWithinApplication(request));
        // A path listed as required is enforced even when the enforcement patterns do not list it: an option
        // that silently does nothing because a path was not repeated in a second list is a trap.
        return !matches(enforcedPatterns, path) && !matches(requiredPatterns, path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getHeader(properties.headerName());
        if (!StringUtils.hasText(key)) {
            PathContainer path = PathContainer.parsePath(pathWithinApplication(request));
            if (properties.required() || matches(requiredPatterns, path)) {
                errorWriter.write(
                        request,
                        response,
                        ErrorCode.IDEMPOTENCY_KEY_REQUIRED,
                        "Missing required header '" + properties.headerName() + "'");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }
        if (key.length() > IdempotencyProperties.MAX_KEY_LENGTH) {
            errorWriter.write(
                    request,
                    response,
                    ErrorCode.BAD_REQUEST,
                    "Header '" + properties.headerName() + "' exceeds " + IdempotencyProperties.MAX_KEY_LENGTH
                            + " characters");
            return;
        }

        byte[] body;
        try {
            body = readBody(request);
        } catch (BodyTooLargeException tooLarge) {
            errorWriter.write(request, response, ErrorCode.PAYLOAD_TOO_LARGE, tooLarge.getMessage());
            return;
        }
        if (body == null) {
            // Declared length already above the cap: leave the stream untouched for the handler.
            filterChain.doFilter(request, response);
            return;
        }

        String path = pathWithinApplication(request);
        IdempotencyRequest attempt = new IdempotencyRequest(
                scopeResolver.resolveScope(request),
                key,
                request.getMethod(),
                path,
                fingerprint(request.getMethod(), path, body),
                Instant.now().plus(properties.retention()));

        IdempotencyClaim claim = store.claim(attempt);
        if (claim instanceof IdempotencyClaim.Replay replay) {
            writeStored(response, replay.response());
            return;
        }
        if (claim instanceof IdempotencyClaim.InProgress) {
            log.warn("{} {} rejected: IDEMPOTENCY_IN_PROGRESS (key {})", request.getMethod(), path, key);
            errorWriter.write(
                    request,
                    response,
                    ErrorCode.IDEMPOTENCY_IN_PROGRESS,
                    "An identical request is still in progress; retry shortly");
            return;
        }
        if (claim instanceof IdempotencyClaim.Mismatch) {
            log.warn("{} {} rejected: IDEMPOTENCY_KEY_REUSED (key {})", request.getMethod(), path, key);
            errorWriter.write(
                    request,
                    response,
                    ErrorCode.IDEMPOTENCY_KEY_REUSED,
                    "Header '" + properties.headerName() + "' was already used for a different request");
            return;
        }

        long claimId = ((IdempotencyClaim.Claimed) claim).id();
        ContentCachingResponseWrapper captured = new ContentCachingResponseWrapper(response);
        boolean settled = false;
        try {
            filterChain.doFilter(new CachedBodyRequest(request, body), captured);
            settled = storeResult(claimId, captured);
        } finally {
            if (!settled) {
                release(claimId, request, path);
            }
            captured.copyBodyToResponse();
        }
    }

    /** @return true when the response was stored, false when the claim must be released for a retry */
    private boolean storeResult(long claimId, ContentCachingResponseWrapper captured) {
        int status = captured.getStatus();
        byte[] payload = captured.getContentAsByteArray();
        if (!isReplayable(status)) {
            return false;
        }
        if (payload.length > properties.maxBodyBytes()) {
            log.warn(
                    "response of {} byte(s) exceeds the idempotency cap and will not be replayed (claim {})",
                    payload.length,
                    claimId);
            return false;
        }
        Charset charset = resolveCharset(captured.getCharacterEncoding());
        store.complete(claimId, new StoredResponse(status, captured.getContentType(), new String(payload, charset)));
        return true;
    }

    /**
     * A response worth replaying is one that describes the *outcome of the operation*. Server faults and
     * retryable throttling responses do not: keeping them would pin a transient failure to the key.
     */
    private boolean isReplayable(int status) {
        if (status < 200 || status >= 500) {
            return false;
        }
        return status != HttpStatus.REQUEST_TIMEOUT.value() && status != HttpStatus.TOO_MANY_REQUESTS.value();
    }

    private void release(long claimId, HttpServletRequest request, String path) {
        try {
            store.release(claimId);
        } catch (RuntimeException failure) {
            // The key stays reserved for the retention window; the caller still gets its response, so this
            // is a degraded-retry problem, not a request failure.
            log.error("could not release the idempotency claim for {} {}", request.getMethod(), path, failure);
        }
    }

    private void writeStored(HttpServletResponse response, StoredResponse stored) throws IOException {
        response.setStatus(stored.status());
        if (StringUtils.hasText(stored.contentType())) {
            response.setContentType(stored.contentType());
        }
        response.setHeader(REPLAYED_HEADER, "true");
        if (stored.body() != null) {
            response.getWriter().write(stored.body());
        }
    }

    /**
     * Buffers the body for fingerprinting. Returns {@code null} when the declared length is already over
     * the cap (nothing is read, so the handler still sees a fresh stream) and throws when a body of
     * unknown length turns out to be over the cap — at that point the stream has been consumed and the
     * only honest answer is 413.
     */
    private byte[] readBody(HttpServletRequest request) throws IOException, BodyTooLargeException {
        long declared = request.getContentLengthLong();
        if (declared > properties.maxBodyBytes()) {
            return null;
        }
        byte[] raw = request.getInputStream().readNBytes(properties.maxBodyBytes() + 1);
        if (raw.length > properties.maxBodyBytes()) {
            throw new BodyTooLargeException(properties.maxBodyBytes());
        }
        return raw;
    }

    private String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri;
    }

    private List<PathPattern> compile(List<String> patterns) {
        return patterns.stream().map(PATTERN_PARSER::parse).toList();
    }

    private boolean matches(List<PathPattern> patterns, PathContainer path) {
        return patterns.stream().anyMatch(pattern -> pattern.matches(path));
    }

    /** Method and path are part of the fingerprint: the same key on a different route is not a replay. */
    private String fingerprint(String method, String path, byte[] body) {
        return DigestUtils.sha256Hex(
                method + '\n' + path + '\n' + DigestUtils.sha256Hex(new String(body, StandardCharsets.UTF_8)));
    }

    private Charset resolveCharset(String encoding) {
        if (!StringUtils.hasText(encoding)) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (RuntimeException unsupported) {
            return StandardCharsets.UTF_8;
        }
    }

    /** Raised when a body without a declared length exceeds the buffering cap. */
    static final class BodyTooLargeException extends Exception {

        private static final long serialVersionUID = 1L;

        BodyTooLargeException(int limit) {
            super("Request body exceeds the idempotency buffering limit of " + limit + " bytes");
        }
    }
}
