// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.encrypt.aes;

import dev.strata.plugin.api.crypto.ApiPayloadCipher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.web.filter.OncePerRequestFilter;

/** Decrypts base64 AES payloads when {@code X-Strata-Encrypted: 1} is present. */
public class ApiPayloadDecryptFilter extends OncePerRequestFilter {

    public static final String HEADER_ENCRYPTED = "X-Strata-Encrypted";

    private final ApiPayloadCipher cipher;

    public ApiPayloadDecryptFilter(ApiPayloadCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!"1".equals(request.getHeader(HEADER_ENCRYPTED))) {
            chain.doFilter(request, response);
            return;
        }
        byte[] raw = request.getInputStream().readAllBytes();
        if (raw.length == 0) {
            chain.doFilter(request, response);
            return;
        }
        String base64 = new String(raw, StandardCharsets.UTF_8).trim();
        byte[] plain = cipher.decrypt(Base64.getDecoder().decode(base64));
        chain.doFilter(new DecryptedBodyRequestWrapper(request, plain), response);
    }

    static final class DecryptedBodyRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        DecryptedBodyRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream stream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return stream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {}

                @Override
                public int read() {
                    return stream.read();
                }
            };
        }
    }
}
