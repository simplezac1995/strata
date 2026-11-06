// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.support;

/** Cross-module sensitive-word scan (implemented in system-biz). */
public interface SensitiveWordScanner {

    String scanHits(String content);
}
