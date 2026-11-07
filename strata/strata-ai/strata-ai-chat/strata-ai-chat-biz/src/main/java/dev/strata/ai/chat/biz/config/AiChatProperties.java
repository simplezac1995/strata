// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.biz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Public-facing base URL for AI chat attachments.
 *
 * <p>Image attachments are handed to the LLM as an {@code image_url} the model provider fetches over the public
 * internet. Internally a file is served by the relative path {@code /api/v1/system/files/.../download}, which is
 * only reachable from the browser — a remote vision model cannot resolve it. When
 * {@code strata.ai.public-base-url} is set it is prepended so the URL becomes absolute and fetchable; when left
 * empty the relative path is kept (the default, for local development where no public endpoint exists).
 *
 * <p>The value must be an origin only (scheme + host, optional port), e.g. {@code https://admin.example.com}; a
 * trailing slash is tolerated and normalised away so the relative path's leading slash still produces a single slash.
 *
 * @param publicBaseUrl see above
 * @param historyLimit how many most-recent messages are sent back to the model as context (and returned by the
 *     message-history endpoint). An unbounded history would grow prompt tokens without bound on long sessions and
 *     would risk an out-of-memory read on a busy session. Bumped to at least 1 on bind.
 */
@ConfigurationProperties(prefix = "strata.ai")
public record AiChatProperties(@DefaultValue("") String publicBaseUrl, @DefaultValue("30") int historyLimit) {

    public AiChatProperties {
        if (publicBaseUrl == null) {
            publicBaseUrl = "";
        }
        publicBaseUrl = publicBaseUrl.trim();
        while (publicBaseUrl.endsWith("/")) {
            publicBaseUrl = publicBaseUrl.substring(0, publicBaseUrl.length() - 1);
        }
        if (historyLimit < 1) {
            historyLimit = 30;
        }
    }

    /**
     * Prefix a download path with the configured public origin.
     *
     * @param path the relative download path (must start with {@code /})
     * @return the absolute URL when a public base is configured, otherwise the path unchanged
     */
    public String absoluteUrl(String path) {
        if (publicBaseUrl.isEmpty() || path == null) {
            return path;
        }
        return publicBaseUrl + path;
    }
}
