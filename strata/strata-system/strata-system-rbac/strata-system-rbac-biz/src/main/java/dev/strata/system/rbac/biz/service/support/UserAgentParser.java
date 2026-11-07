// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

/** Best-effort browser label for the admin login log (no external user-agent library needed). */
public final class UserAgentParser {

    private UserAgentParser() {}

    public static String browserOf(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String marker = "Firefox/";
        if (userAgent.contains(marker)) {
            return "Firefox " + versionAfter(userAgent, marker);
        }
        marker = "Edg/";
        if (userAgent.contains(marker)) {
            return "Edge " + versionAfter(userAgent, marker);
        }
        marker = "Chrome/";
        if (userAgent.contains(marker)) {
            return "Chrome " + versionAfter(userAgent, marker);
        }
        marker = "Version/";
        if (userAgent.contains("Safari/") && userAgent.contains(marker)) {
            return "Safari " + versionAfter(userAgent, marker);
        }
        return userAgent.length() > 128 ? userAgent.substring(0, 128) : userAgent;
    }

    private static String versionAfter(String value, String marker) {
        int start = value.indexOf(marker) + marker.length();
        int end = start;
        while (end < value.length() && (Character.isDigit(value.charAt(end)) || value.charAt(end) == '.')) {
            end++;
        }
        return start < end ? value.substring(start, end) : "";
    }
}
