// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.web;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Result<T>(int code, String message, T data, String traceId) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data, null);
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public Result<T> withTraceId(String traceId) {
        return new Result<>(code, message, data, traceId);
    }
}
