// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.tenant;

import dev.strata.common.core.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reads {@code X-Tenant-Id} and binds it to {@link TenantContext} for the request lifetime. */
public class TenantContextFilter extends OncePerRequestFilter {

    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    private final TenantProperties tenantProperties;

    public TenantContextFilter(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!tenantProperties.isRowLevelEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        try {
            String raw = request.getHeader(HEADER_TENANT_ID);
            if (raw != null && !raw.isBlank()) {
                TenantContext.setTenantId(Long.parseLong(raw.trim()));
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
