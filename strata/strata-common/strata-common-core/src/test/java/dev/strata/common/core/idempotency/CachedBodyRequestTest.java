// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.ReadListener;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for {@link CachedBodyRequest}: the body survives a double read, the reader honours the
 * request character encoding (with a UTF-8 fallback), and the async-read listener never blocks.
 */
@DisplayName("CachedBodyRequest 可重复读取请求体")
class CachedBodyRequestTest {

    private CachedBodyRequest requestWith(String body, String encoding) {
        MockHttpServletRequest delegate = new MockHttpServletRequest();
        if (encoding != null) {
            delegate.setCharacterEncoding(encoding);
        }
        return new CachedBodyRequest(delegate, body.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("getInputStream 可重复读到完整 body")
    void inputStreamReadsBodyTwice() throws Exception {
        CachedBodyRequest req = requestWith("hello", "UTF-8");

        byte[] first = req.getInputStream().readAllBytes();
        byte[] second = req.getInputStream().readAllBytes();

        assertThat(first).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("分块 read 与 available 行为正确")
    void chunkedReadAndAvailable() throws Exception {
        CachedBodyRequest req = requestWith("abcdef", "UTF-8");

        assertThat(req.getInputStream().available()).isEqualTo(6);

        byte[] buf = new byte[3];
        int n = req.getInputStream().read(buf, 0, 3);

        assertThat(n).isEqualTo(3);
        assertThat(new String(buf, StandardCharsets.UTF_8)).isEqualTo("abc");
    }

    @Test
    @DisplayName("getReader 按请求字符集解码")
    void readerHonoursCharset() throws Exception {
        CachedBodyRequest req = requestWith("héllo", "ISO-8859-1");

        BufferedReader reader = req.getReader();

        // The cached bytes are UTF-8; a Latin-1 view of them is a different string, which proves the
        // reader used the request-declared charset rather than always assuming UTF-8.
        assertThat(reader.readLine()).isNotEqualTo("héllo");
    }

    @Test
    @DisplayName("未设置字符集时回退 UTF-8")
    void missingEncodingFallsBackToUtf8() throws Exception {
        CachedBodyRequest req = requestWith("héllo", null);

        assertThat(req.getReader().readLine()).isEqualTo("héllo");
    }

    @Test
    @DisplayName("非法字符集回退 UTF-8 不抛异常")
    void invalidEncodingFallsBackToUtf8() throws Exception {
        CachedBodyRequest req = requestWith("ok", "no-such-charset");

        assertThat(req.getReader().readLine()).isEqualTo("ok");
    }

    @Test
    @DisplayName("isReady 恒为 true、空体 isFinished 为 true")
    void asyncStateNeverBlocks() throws Exception {
        CachedBodyRequest nonEmpty = requestWith("x", "UTF-8");
        CachedBodyRequest empty = requestWith("", "UTF-8");

        assertThat(nonEmpty.getInputStream().isReady()).isTrue();
        assertThat(nonEmpty.getInputStream().isFinished()).isFalse();
        assertThat(empty.getInputStream().isFinished()).isTrue();
    }

    @Test
    @DisplayName("setReadListener 空体立即回调、非空体不回调、null 安全")
    void setReadListenerCallbacks() throws Exception {
        CachedBodyRequest empty = requestWith("", "UTF-8");
        ReadListener emptyListener = org.mockito.Mockito.mock(ReadListener.class);
        empty.getInputStream().setReadListener(emptyListener);
        verify(emptyListener).onAllDataRead();

        CachedBodyRequest nonEmpty = requestWith("x", "UTF-8");
        ReadListener busyListener = org.mockito.Mockito.mock(ReadListener.class);
        nonEmpty.getInputStream().setReadListener(busyListener);
        verify(busyListener, never()).onAllDataRead();

        nonEmpty.getInputStream().setReadListener(null);
    }
}
