// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.api.storage;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Object storage SPI. Implementations live in plugin starters.
 */
public interface ObjectStorage {

    void put(String bucket, String objectKey, InputStream content, String contentType);

    Optional<InputStream> get(String bucket, String objectKey);

    void delete(String bucket, String objectKey);

    List<ObjectInfo> list(String bucket);

    List<String> buckets();
}
