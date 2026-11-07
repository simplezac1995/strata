// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.storage.local;

import dev.strata.plugin.api.storage.ObjectInfo;
import dev.strata.plugin.api.storage.ObjectStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Local filesystem implementation of the ObjectStorage SPI (dev/self-host use). */
public class LocalObjectStorage implements ObjectStorage {

    private final Path root;

    public LocalObjectStorage(LocalStorageProperties properties) {
        this.root = Paths.get(properties.baseDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to create storage root " + this.root, ex);
        }
    }

    @Override
    public void put(String bucket, String objectKey, InputStream content, String contentType) {
        Path target = resolve(bucket, objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store object " + objectKey, ex);
        }
    }

    @Override
    public Optional<InputStream> get(String bucket, String objectKey) {
        Path target = resolve(bucket, objectKey);
        if (!Files.exists(target)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.newInputStream(target));
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read object " + objectKey, ex);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            Files.deleteIfExists(resolve(bucket, objectKey));
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to delete object " + objectKey, ex);
        }
    }

    @Override
    public List<ObjectInfo> list(String bucket) {
        Path dir = bucketDir(bucket);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (var paths = Files.walk(dir)) {
            return paths.filter(Files::isRegularFile)
                    .map(path -> {
                        String key = root.relativize(path).toString().replace('\\', '/');
                        key = key.substring(key.indexOf('/') + 1);
                        long size = path.toFile().length();
                        String contentType = null;
                        try {
                            contentType = Files.probeContentType(path);
                        } catch (IOException ignored) {
                            // probeContentType is best-effort
                        }
                        Instant lastModified =
                                Instant.ofEpochMilli(path.toFile().lastModified());
                        return new ObjectInfo(key, size, contentType, lastModified);
                    })
                    .sorted(Comparator.comparing(ObjectInfo::key))
                    .toList();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to list bucket " + bucket, ex);
        }
    }

    @Override
    public List<String> buckets() {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (var paths = Files.list(root)) {
            return paths.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to list buckets", ex);
        }
    }

    private Path bucketDir(String bucket) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("bucket is required");
        }
        Path dir = root.resolve(bucket).normalize();
        if (!dir.startsWith(root)) {
            throw new IllegalArgumentException("bucket escapes the storage root");
        }
        return dir;
    }

    private Path resolve(String bucket, String objectKey) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("bucket is required");
        }
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey is required");
        }
        Path target = root.resolve(bucket).resolve(objectKey).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("objectKey escapes the storage root");
        }
        return target;
    }
}
