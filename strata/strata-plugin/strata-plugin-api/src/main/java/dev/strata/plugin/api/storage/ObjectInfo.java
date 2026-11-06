// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.api.storage;

import java.time.Instant;

public record ObjectInfo(String key, long size, String contentType, Instant lastModified) {}
