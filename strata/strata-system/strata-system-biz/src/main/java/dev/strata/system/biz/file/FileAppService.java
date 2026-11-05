// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.file;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.plugin.api.storage.ObjectStorage;
import dev.strata.system.api.dto.FileInfoDto;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileAppService {

    private static final Logger log = LoggerFactory.getLogger(FileAppService.class);

    private static final String DEFAULT_BUCKET = "uploads";

    /**
     * P1-17: upload allow-list. Only these content types (and matching extensions) are accepted. SVG / HTML are
     * explicitly rejected so an uploaded file cannot be rendered inline as active content.
     */
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/png", "image/jpeg", "image/gif", "image/webp", "application/pdf");

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp", "pdf");

    private final ObjectProvider<ObjectStorage> storageProvider;

    public FileAppService(ObjectProvider<ObjectStorage> storageProvider) {
        this.storageProvider = storageProvider;
    }

    @Transactional(readOnly = true)
    public List<FileInfoDto> list() {
        ObjectStorage storage = storage();
        return storage.buckets().stream()
                .flatMap(bucket -> storage.list(bucket).stream()
                        .map(info -> new FileInfoDto(
                                info.key(), bucket, info.size(), info.contentType(), info.lastModified())))
                .sorted(Comparator.comparing(FileInfoDto::lastModified).reversed())
                .toList();
    }

    @OperationLog(module = "system:file", action = "create")
    public FileInfoDto upload(MultipartFile file, String bucket) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File is empty");
        }
        validateAllowedType(file);
        String targetBucket = bucket == null || bucket.isBlank() ? DEFAULT_BUCKET : sanitize(bucket);
        String key = sanitize(file.getOriginalFilename());
        if (key.isBlank()) {
            key = UUID.randomUUID().toString();
        }
        try {
            storage().put(targetBucket, key, file.getInputStream(), file.getContentType());
        } catch (IOException ex) {
            log.error("Error in FileAppService: {}", ex.getMessage(), ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to read upload");
        }
        return new FileInfoDto(key, targetBucket, file.getSize(), file.getContentType(), Instant.now());
    }

    /** Streams the object without buffering it in memory; bucket/key come from the URL path and are validated. */
    public Resource download(String bucket, String key) {
        String targetBucket = requireSafe(bucket);
        String targetKey = requireSafe(key);
        InputStream stream =
                storage().get(targetBucket, targetKey).orElseThrow(() -> new NotFoundException("File not found"));
        return new InputStreamResource(stream);
    }

    @OperationLog(module = "system:file", action = "delete")
    public void delete(String bucket, String key) {
        storage().delete(requireSafe(bucket), requireSafe(key));
    }

    private ObjectStorage storage() {
        ObjectStorage storage = storageProvider.getIfAvailable();
        if (storage == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "No object storage configured");
        }
        return storage;
    }

    /** Validates a caller-supplied bucket/key and rejects path traversal or unsafe characters. */
    private static String requireSafe(String value) {
        String cleaned = sanitize(value);
        if (cleaned.isBlank() || !cleaned.equals(value) || ".".equals(cleaned) || "..".equals(cleaned)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid bucket or key");
        }
        return cleaned;
    }

    /** Keeps only safe filename characters; prevents path traversal. */
    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String cleaned = value.replace('\\', '/');
        int slash = cleaned.lastIndexOf('/');
        if (slash >= 0) {
            cleaned = cleaned.substring(slash + 1);
        }
        return cleaned.replaceAll("[^A-Za-z0-9._-]", "");
    }

    /**
     * P1-17: enforce the upload allow-list on both the declared content type and the filename extension. The
     * content type is client-supplied, so the extension is checked as a second signal (and vice-versa). A blank
     * filename (which becomes a UUID key) has no extension and is governed by the content type only.
     */
    private static void validateAllowedType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            int semi = contentType.indexOf(';');
            if (semi >= 0) {
                contentType = contentType.substring(0, semi);
            }
            contentType = contentType.trim().toLowerCase(Locale.ROOT);
        }
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported file type: " + file.getContentType());
        }
        String original = file.getOriginalFilename();
        if (original != null) {
            int dot = original.lastIndexOf('.');
            if (dot >= 0 && dot < original.length() - 1) {
                String ext = original.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (!ALLOWED_EXTENSIONS.contains(ext)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported file extension: ." + ext);
                }
            }
        }
    }
}
