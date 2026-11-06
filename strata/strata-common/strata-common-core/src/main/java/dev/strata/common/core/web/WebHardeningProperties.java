// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Web-layer request limits that the embedded container's form-post knobs do not cover.
 *
 * <p>{@code server.tomcat.max-http-form-post-size} only bounds {@code application/x-www-form-urlencoded} bodies;
 * a JSON API that accepts {@code application/json} had no server-side ceiling, so a request claiming a multi-gigabyte
 * JSON body could reach the message converter before anything else rejected it. This bounds the declared JSON body
 * size cheaply, before it is read.
 *
 * @param jsonMaxBytes maximum accepted {@code application/json} request body in bytes (declared Content-Length).
 *     Defaults to 2 MiB. Chunked (unknown-length) bodies are not buffered here; the container's
 *     {@code max-swallow-size} is the backstop for those.
 */
@ConfigurationProperties(prefix = "strata.web")
public record WebHardeningProperties(
        @DefaultValue("true") boolean jsonBodyLimitEnabled, @DefaultValue("2097152") int jsonMaxBytes) {

    public WebHardeningProperties {
        if (jsonMaxBytes <= 0) {
            jsonMaxBytes = 2 * 1024 * 1024;
        }
    }
}
