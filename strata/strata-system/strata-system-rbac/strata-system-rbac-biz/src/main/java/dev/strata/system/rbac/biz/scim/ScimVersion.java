// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.scim;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Stable resource version used for {@code meta.version} and HTTP {@code ETag} (change a field -> new version). */
final class ScimVersion {

    private ScimVersion() {}

    static String of(Object... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder input = new StringBuilder();
            for (Object value : values) {
                input.append(value == null ? "" : value).append('|');
            }
            return '"'
                    + HexFormat.of().formatHex(digest.digest(input.toString().getBytes(StandardCharsets.UTF_8)))
                    + '"';
        } catch (Exception exception) {
            throw new IllegalStateException("Could not compute the SCIM resource version", exception);
        }
    }
}
