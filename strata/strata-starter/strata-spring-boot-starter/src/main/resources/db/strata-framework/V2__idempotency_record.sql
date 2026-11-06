-- Framework schema v2: idempotency store used by IdempotencyFilter (POST/PUT/PATCH/DELETE under /api/v1/**).
-- Kept as a separate migration because V1 is already released and migrations are immutable.
CREATE TABLE IF NOT EXISTS sys_idempotency_record (
    id                    BIGINT       NOT NULL PRIMARY KEY,
    scope                 VARCHAR(64)  NOT NULL,
    idempotency_key       VARCHAR(128) NOT NULL,
    request_method        VARCHAR(8)   NOT NULL,
    request_path          VARCHAR(256) NOT NULL,
    request_fingerprint   CHAR(64)     NOT NULL,
    status                VARCHAR(16)  NOT NULL,
    response_status       SMALLINT,
    response_content_type VARCHAR(128),
    response_body         TEXT,
    created_at            TIMESTAMP    NOT NULL,
    completed_at          TIMESTAMP,
    expires_at            TIMESTAMP    NOT NULL,
    CONSTRAINT uk_sys_idempotency_scope_key UNIQUE (scope, idempotency_key)
);
CREATE INDEX IF NOT EXISTS idx_sys_idempotency_expires_at ON sys_idempotency_record (expires_at);
CREATE INDEX IF NOT EXISTS idx_sys_idempotency_created_at ON sys_idempotency_record (created_at);
