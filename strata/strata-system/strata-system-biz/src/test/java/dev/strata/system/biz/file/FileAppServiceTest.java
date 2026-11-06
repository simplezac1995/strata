// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.plugin.api.storage.ObjectInfo;
import dev.strata.plugin.api.storage.ObjectStorage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileAppService 单元测试")
class FileAppServiceTest {

    @Mock
    private ObjectProvider<ObjectStorage> storageProvider;

    @Mock
    private ObjectStorage storage;

    @Mock
    private MultipartFile multipartFile;

    private FileAppService service;

    @BeforeEach
    void setUp() {
        service = new FileAppService(storageProvider);
    }

    private void stubStorage() {
        when(storageProvider.getIfAvailable()).thenReturn(storage);
    }

    @Nested
    @DisplayName("list - 列出文件")
    class ListTests {

        @Test
        @DisplayName("跨桶列出并按修改时间倒序")
        void list_AcrossBuckets_SortedDesc() {
            stubStorage();
            Instant older = Instant.parse("2024-01-01T00:00:00Z");
            Instant newer = Instant.parse("2024-06-01T00:00:00Z");
            when(storage.buckets()).thenReturn(List.of("b1", "b2"));
            when(storage.list("b1")).thenReturn(List.of(new ObjectInfo("a.txt", 10L, "text/plain", older)));
            when(storage.list("b2")).thenReturn(List.of(new ObjectInfo("b.txt", 20L, "text/plain", newer)));

            var result = service.list();

            assertThat(result).hasSize(2);
            // 最新的 b.txt（newer）排第一
            assertThat(result.get(0).key()).isEqualTo("b.txt");
            assertThat(result.get(0).bucket()).isEqualTo("b2");
            assertThat(result.get(1).key()).isEqualTo("a.txt");
            assertThat(result.get(1).bucket()).isEqualTo("b1");
        }
    }

    @Nested
    @DisplayName("upload - 上传")
    class UploadTests {

        @Test
        @DisplayName("文件为 null 时抛 BAD_REQUEST")
        void upload_NullFile_Throws() {
            assertThatThrownBy(() -> service.upload(null, "uploads"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("空文件时抛 BAD_REQUEST")
        void upload_EmptyFile_Throws() {
            when(multipartFile.isEmpty()).thenReturn(true);

            assertThatThrownBy(() -> service.upload(multipartFile, "uploads"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("桶名为空时使用默认 uploads 桶")
        void upload_BlankBucket_UsesDefaultBucket() throws IOException {
            stubStorage();
            stubValidFile("photo.png", "image/png", 123L, new byte[] {1, 2});

            service.upload(multipartFile, "  ");

            verify(storage).put(eq("uploads"), eq("photo.png"), any(InputStream.class), eq("image/png"));
        }

        @Test
        @DisplayName("文件名含路径时剥离目录部分防穿越")
        void upload_PathTraversal_StripsDirectory() throws IOException {
            stubStorage();
            stubValidFile("/etc/passwd/../../evil.png", "image/png", 10L, new byte[] {9});

            service.upload(multipartFile, "uploads");

            verify(storage).put(eq("uploads"), eq("evil.png"), any(InputStream.class), eq("image/png"));
        }

        @Test
        @DisplayName("文件名为空时用 UUID 作为 key")
        void upload_BlankFilename_UsesUuid() throws IOException {
            stubStorage();
            stubValidFile("   ", "image/png", 5L, new byte[] {0});
            org.mockito.ArgumentCaptor<String> keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);

            service.upload(multipartFile, "uploads");

            verify(storage).put(eq("uploads"), keyCaptor.capture(), any(InputStream.class), eq("image/png"));
            assertThat(keyCaptor.getValue()).isNotBlank();
        }

        @Test
        @DisplayName("读取流抛 IOException 时抛 INTERNAL_ERROR")
        void upload_IoException_ThrowsInternalError() throws IOException {
            stubStorage();
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn("x.png");
            when(multipartFile.getContentType()).thenReturn("image/png");
            when(multipartFile.getInputStream()).thenThrow(new IOException("disk full"));

            assertThatThrownBy(() -> service.upload(multipartFile, "uploads"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
        }

        @Test
        @DisplayName("P1-17: image/svg+xml 内容类型被拒绝")
        void upload_SvgContentType_Rejected() {
            // Rejection happens in the type allow-list before storage is touched; no storage stub needed.
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn("image/svg+xml");

            assertThatThrownBy(() -> service.upload(multipartFile, "uploads"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
            verify(storage, never()).put(any(), any(), any(), any());
        }

        @Test
        @DisplayName("P1-17: text/html 内容类型被拒绝")
        void upload_HtmlContentType_Rejected() {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn("text/html");

            assertThatThrownBy(() -> service.upload(multipartFile, "uploads"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
            verify(storage, never()).put(any(), any(), any(), any());
        }

        @Test
        @DisplayName("P1-17: MIME 伪装但扩展名为 .html 时仍被拒绝")
        void upload_ContentSpoofedExtension_Rejected() {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn("evil.html");
            when(multipartFile.getContentType()).thenReturn("image/png");

            assertThatThrownBy(() -> service.upload(multipartFile, "uploads"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
            verify(storage, never()).put(any(), any(), any(), any());
        }

        private void stubValidFile(String filename, String contentType, long size, byte[] content) throws IOException {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn(filename);
            when(multipartFile.getContentType()).thenReturn(contentType);
            when(multipartFile.getSize()).thenReturn(size);
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));
        }
    }

    @Nested
    @DisplayName("download - 下载")
    class DownloadTests {

        @Test
        @DisplayName("成功下载返回流资源")
        void download_Success_ReturnsStream() {
            stubStorage();
            InputStream stream = new ByteArrayInputStream("data".getBytes());
            when(storage.get("uploads", "a.png")).thenReturn(Optional.of(stream));

            Resource resource = service.download("uploads", "a.png");

            assertThat(resource).isNotNull();
        }

        @Test
        @DisplayName("对象不存在时抛 NOT_FOUND")
        void download_NotFound_Throws() {
            stubStorage();
            when(storage.get(any(), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.download("uploads", "missing.png"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("桶名含路径穿越时抛 BAD_REQUEST")
        void download_PathTraversalBucket_Throws() {
            assertThatThrownBy(() -> service.download("../etc", "a.png"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("key 为 .. 时抛 BAD_REQUEST")
        void download_DotDotKey_Throws() {
            assertThatThrownBy(() -> service.download("uploads", ".."))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("key 含特殊字符被清洗后与原值不等时抛 BAD_REQUEST")
        void download_DirtyKey_Throws() {
            assertThatThrownBy(() -> service.download("uploads", "a b.png"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        }
    }

    @Nested
    @DisplayName("delete - 删除")
    class DeleteTests {

        @Test
        @DisplayName("成功删除")
        void delete_Success() {
            stubStorage();

            service.delete("uploads", "a.png");

            verify(storage).delete("uploads", "a.png");
        }
    }

    @Nested
    @DisplayName("storage() 未配置")
    class NoStorageTests {

        @Test
        @DisplayName("未配置存储时 list 抛 INTERNAL_ERROR")
        void list_NoStorage_Throws() {
            when(storageProvider.getIfAvailable()).thenReturn(null);

            assertThatThrownBy(() -> service.list())
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
        }

        @Test
        @DisplayName("未配置存储时 download 抛 INTERNAL_ERROR")
        void download_NoStorage_Throws() {
            when(storageProvider.getIfAvailable()).thenReturn(null);

            assertThatThrownBy(() -> service.download("uploads", "a.png"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));

            verify(storage, never()).delete(any(), any());
        }
    }
}
