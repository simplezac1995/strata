// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import dev.strata.common.core.util.IdGenerator;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PostgreSQL JDBC-backed store: the concurrency guard is a plain unique index, and a duplicate insert is caught
 * and classified instead of depending on dialect-specific upsert syntax.
 */
public class JdbcIdempotencyStore implements IdempotencyStore {

    private static final Logger log = LoggerFactory.getLogger(JdbcIdempotencyStore.class);

    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String COMPLETED = "COMPLETED";

    private static final String INSERT =
            """
            INSERT INTO sys_idempotency_record
                (id, scope, idempotency_key, request_method, request_path, request_fingerprint,
                 status, created_at, expires_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT =
            """
            SELECT id, request_fingerprint, status, response_status, response_content_type, response_body
            FROM sys_idempotency_record
            WHERE scope = ? AND idempotency_key = ?
            """;

    private static final String COMPLETE =
            """
            UPDATE sys_idempotency_record
            SET status = ?, response_status = ?, response_content_type = ?, response_body = ?, completed_at = ?
            WHERE id = ? AND status = ?
            """;

    /** Only unfinished claims are deletable: a completed record is the whole point of the table. */
    private static final String RELEASE = "DELETE FROM sys_idempotency_record WHERE id = ? AND status = ?";

    private static final String PURGE = "DELETE FROM sys_idempotency_record WHERE expires_at < ?";

    private final JdbcTemplate jdbc;

    public JdbcIdempotencyStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public IdempotencyClaim claim(IdempotencyRequest request) {
        long id = IdGenerator.next();
        Instant now = Instant.now();
        try {
            jdbc.update(
                    INSERT,
                    id,
                    request.scope(),
                    request.key(),
                    request.method(),
                    request.path(),
                    request.fingerprint(),
                    IN_PROGRESS,
                    Timestamp.from(now),
                    Timestamp.from(request.expiresAt()));
            return new IdempotencyClaim.Claimed(id);
        } catch (DuplicateKeyException duplicate) {
            return classify(request);
        }
    }

    @Override
    public void complete(long id, StoredResponse response) {
        int updated = jdbc.update(
                COMPLETE,
                COMPLETED,
                response.status(),
                response.contentType(),
                response.body(),
                Timestamp.from(Instant.now()),
                id,
                IN_PROGRESS);
        if (updated == 0) {
            // The claim was purged between insert and completion: log it, but the caller already has its
            // answer, so nothing else can be done here.
            log.warn("idempotency record {} could not be completed (purged concurrently)", id);
        }
    }

    @Override
    public void release(long id) {
        jdbc.update(RELEASE, id, IN_PROGRESS);
    }

    @Override
    public int purgeExpired(Instant now) {
        int deleted = jdbc.update(PURGE, Timestamp.from(now));
        if (deleted > 0) {
            log.debug("purged {} expired idempotency record(s)", deleted);
        }
        return deleted;
    }

    /**
     * Decides what a duplicate insert means. The row can vanish between the failed insert and this read
     * (a concurrent release or purge), in which case the key is free again and the caller may retry the
     * insert — that retry is bounded, so a pathological loop cannot hang a request.
     */
    private IdempotencyClaim classify(IdempotencyRequest request) {
        for (int attempt = 0; attempt < 2; attempt++) {
            List<Stored> rows = jdbc.query(
                    SELECT,
                    (rs, rowNum) -> new Stored(
                            rs.getString("request_fingerprint"),
                            rs.getString("status"),
                            rs.getObject("response_status") == null ? null : rs.getInt("response_status"),
                            rs.getString("response_content_type"),
                            rs.getString("response_body")),
                    request.scope(),
                    request.key());
            if (rows.isEmpty()) {
                continue;
            }
            Stored stored = rows.getFirst();
            if (!stored.fingerprint().equals(request.fingerprint())) {
                return new IdempotencyClaim.Mismatch();
            }
            if (COMPLETED.equals(stored.status()) && stored.responseStatus() != null) {
                return new IdempotencyClaim.Replay(
                        new StoredResponse(stored.responseStatus(), stored.contentType(), stored.body()));
            }
            return new IdempotencyClaim.InProgress();
        }
        return new IdempotencyClaim.InProgress();
    }

    private record Stored(String fingerprint, String status, Integer responseStatus, String contentType, String body) {}
}
