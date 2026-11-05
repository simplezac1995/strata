// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

/**
 * A freshly issued admin login captcha challenge.
 *
 * <p>The server already knows the numeric answer and has stored it under {@code captchaId} with a short TTL; the
 * client only renders {@code question} (e.g. {@code "7 + 4 = ?"}) back to the user and echoes {@code captchaId}
 * together with the answer on the next login request.
 *
 * @param captchaId opaque id the client echoes back on login
 * @param question the human-readable arithmetic question to display
 */
public record CaptchaIssueResponse(String captchaId, String question) {}
