// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Request whose body can be read twice: the filter has to fingerprint the payload *before* calling
 * downstream, and the controller still has to receive it intact.
 *
 * <p>{@code ContentCachingRequestWrapper} is not usable here — it caches what downstream reads, so
 * reading the body first would leave the controller an exhausted stream.
 */
class CachedBodyRequest extends HttpServletRequestWrapper {

    private final byte[] body;
    private final Charset charset;

    CachedBodyRequest(HttpServletRequest request, byte[] body) {
        super(request);
        this.body = body;
        Charset encoding;
        try {
            encoding = request.getCharacterEncoding() == null
                    ? StandardCharsets.UTF_8
                    : Charset.forName(request.getCharacterEncoding());
        } catch (RuntimeException unsupported) {
            encoding = StandardCharsets.UTF_8;
        }
        this.charset = encoding;
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(body);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }

    /** A second read still sees the whole body, because the bytes live in memory. */
    private static final class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream delegate;

        private CachedBodyServletInputStream(byte[] body) {
            this.delegate = new ByteArrayInputStream(body);
        }

        @Override
        public int read() {
            return delegate.read();
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            return delegate.read(bytes, offset, length);
        }

        @Override
        public int available() {
            return delegate.available();
        }

        @Override
        public boolean isFinished() {
            return delegate.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        /** The body is already in memory, so there is nothing to notify: async reads never block. */
        @Override
        public void setReadListener(ReadListener readListener) {
            try {
                if (readListener != null && isFinished()) {
                    readListener.onAllDataRead();
                }
            } catch (IOException ignored) {
                // Nothing can fail here, and the servlet API declares the checked exception.
            }
        }
    }
}
