// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.system.ops.api.entity.ApiCallStat;
import dev.strata.system.ops.biz.repository.ApiCallStatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Writes {@link ApiCallStat} rows off the request thread. The filter hands a captured sample to this bean after
 * the response has been committed; a failure here must never surface to the caller, so every write is wrapped in
 * a best-effort try/catch.
 */
@Component
public class ApiCallStatRecorder {

    private static final Logger log = LoggerFactory.getLogger(ApiCallStatRecorder.class);

    private final ApiCallStatRepository repository;

    public ApiCallStatRecorder(ApiCallStatRepository repository) {
        this.repository = repository;
    }

    /** Best-effort asynchronous insert; sampling is 100% this iteration. */
    @Async("auditLogExecutor")
    public void record(String path, String method, int statusCode, long durationMs, String ip) {
        try {
            ApiCallStat row = new ApiCallStat();
            row.setPath(path);
            row.setMethod(method);
            row.setStatusCode(statusCode);
            row.setDurationMs(durationMs);
            row.setIp(ip);
            repository.save(row);
        } catch (Exception e) {
            log.warn("Failed to record api call stat for {} {}: {}", method, path, e.getMessage());
        }
    }
}
