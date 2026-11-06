// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.event;

/** Published on every login attempt (success / failure / locked). */
public record LoginLogEvent(String username, String ip, String browser, String status) {}
