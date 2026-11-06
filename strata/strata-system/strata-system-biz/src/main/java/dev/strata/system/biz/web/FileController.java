// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.FileInfoDto;
import dev.strata.system.biz.file.FileAppService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/system/files")
public class FileController {

    private final FileAppService fileAppService;

    public FileController(FileAppService fileAppService) {
        this.fileAppService = fileAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_FILE_LIST)
    public Result<List<FileInfoDto>> list() {
        return Result.ok(fileAppService.list());
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_FILE_CREATE)
    public Result<FileInfoDto> upload(
            @RequestPart("file") MultipartFile file, @RequestParam(defaultValue = "uploads") String bucket) {
        return Result.ok(fileAppService.upload(file, bucket));
    }

    @GetMapping("/{bucket}/{key}/download")
    @PreAuthorize(Permissions.SYSTEM_FILE_LIST)
    public ResponseEntity<Resource> download(@PathVariable String bucket, @PathVariable String key) {
        Resource resource = fileAppService.download(bucket, key);
        String filename = key.replaceAll("[^A-Za-z0-9._-]", "_");
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{bucket}/{key}")
    @PreAuthorize(Permissions.SYSTEM_FILE_DELETE)
    public Result<Void> delete(@PathVariable String bucket, @PathVariable String key) {
        fileAppService.delete(bucket, key);
        return Result.ok();
    }
}
