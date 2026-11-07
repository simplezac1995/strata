// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.storage.local;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.plugin.api.storage.ObjectStorage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalObjectStorageTest {

    @TempDir
    Path tempDir;

    private ObjectStorage storage() {
        return new LocalObjectStorage(new LocalStorageProperties(tempDir.toString()));
    }

    @Test
    void put_and_get_roundtrip() throws Exception {
        ObjectStorage storage = storage();
        storage.put(
                "avatars", "u/1.png", new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), "image/png");

        Optional<java.io.InputStream> result = storage.get("avatars", "u/1.png");
        assertThat(result).isPresent();
        try (java.io.InputStream in = result.get()) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("hello");
        }
    }

    @Test
    void get_returns_empty_for_missing_key() {
        assertThat(storage().get("avatars", "missing.png")).isEmpty();
    }

    @Test
    void delete_removes_object() throws Exception {
        ObjectStorage storage = storage();
        storage.put("files", "a.txt", new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)), "text/plain");
        storage.delete("files", "a.txt");
        assertThat(storage.get("files", "a.txt")).isEmpty();
        assertThat(Files.exists(tempDir.resolve("files/a.txt"))).isFalse();
    }

    @Test
    void rejects_path_traversal() {
        ObjectStorage storage = storage();
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> storage.put(
                        "b",
                        "../../escape.txt",
                        new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)),
                        "text/plain"));
    }
}
