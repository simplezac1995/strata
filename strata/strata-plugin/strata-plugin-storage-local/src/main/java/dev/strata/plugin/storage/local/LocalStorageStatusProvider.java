// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.storage.local;

import dev.strata.plugin.api.status.PluginStatusProvider;
import dev.strata.plugin.api.storage.ObjectStorage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Self-declares the {@code storageLocal} status key (ADR-0020).
 *
 * <p>Registered only while the storage-local module is enabled — mirroring {@link
 * LocalStorageAutoConfiguration} — so the key is omitted from {@code /plugins/status} when the
 * switch is off. Availability tracks the {@link ObjectStorage} bean the module provides.
 */
@Component
@Order(10)
@ConditionalOnProperty(prefix = "strata.plugin.storage.local", name = "enabled", havingValue = "true")
public class LocalStorageStatusProvider implements PluginStatusProvider {

    private final ObjectProvider<ObjectStorage> objectStorage;

    public LocalStorageStatusProvider(ObjectProvider<ObjectStorage> objectStorage) {
        this.objectStorage = objectStorage;
    }

    @Override
    public String key() {
        return "storageLocal";
    }

    @Override
    public String label() {
        return "Local object storage";
    }

    @Override
    public boolean available() {
        return objectStorage.getIfAvailable() != null;
    }

    @Override
    public String detail() {
        return "Local filesystem ObjectStorage backend";
    }
}
