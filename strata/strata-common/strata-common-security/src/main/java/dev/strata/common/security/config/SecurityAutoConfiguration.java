// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.config;

import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.security.PermitPathContributor;
import dev.strata.common.core.web.ProblemDetails;
import dev.strata.common.core.web.TraceIdFilter;
import dev.strata.common.security.jwt.JwtAuthenticationFilter;
import dev.strata.common.security.jwt.JwtProperties;
import dev.strata.common.security.jwt.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
@Import(SecurityExceptionHandler.class)
public class SecurityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityAutoConfiguration.class);

    /** Core default paths that never require login. Module/webhook paths come from PermitPathContributor (ADR-0021). */
    private static final String[] CORE_PERMIT_PATHS = {
        // Management probes: /actuator/health/liveness and /readiness are separate groups on the
        // management port; a 401 here would make every orchestrator probe fail and kill a healthy pod.
        "/actuator/health/**",
        "/actuator/info",
        "/actuator/prometheus",
        "/api/v1/system/auth/login",
        "/api/v1/legal",
        "/api/v1/system/auth/captcha",
        "/api/v1/system/auth/refresh",
        "/api/v1/system/auth/register",
        "/api/v1/system/auth/forgot-password",
        "/api/v1/system/auth/reset-password",
        "/api/v1/system/auth/verify-email",
        "/api/v1/system/auth/mfa/verify",
        // Browsers POST CSP violation reports without any credential (P2-11).
        "/api/v1/system/security/csp-report",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**",
    };

    /**
     * Public SPA static files. The Vite build (strata-ui/dist) is baked into the app image under
     * classpath:/static (CI copies it in before the image build) and Caddy only reverse-proxies, so this API
     * process itself serves the admin UI and its assets must load before anyone can authenticate.
     */
    private static final String[] SPA_STATIC_PATHS = {
        "/",
        "/index.html",
        "/offline.html",
        "/favicon.ico",
        "/favicon.svg",
        "/icons.svg",
        "/robots.txt",
        "/manifest.webmanifest",
        "/manifest.json",
        "/sw.js",
        "/assets/**",
    };

    /**
     * Prefixes owned by JSON APIs, management endpoints and API docs. A GET under one of these is never a
     * browser navigation route and must not be answered with the SPA shell (see {@link #isSpaNavigation}).
     */
    private static final List<String> NON_SPA_PREFIXES =
            List.of("/api/", "/v1/", "/actuator/", "/v3/", "/swagger-ui", "/swagger-resources", "/webjars/");

    /**
     * BrowserRouter deep links (e.g. {@code /system/users}) are client-side routes with no controller and no
     * static file; the SPA forwarding advice serves them {@code index.html}. Such a GET is public because the
     * UI shell carries no data — every JSON call under {@code /api} or {@code /v1} still requires a credential.
     * Write methods are deliberately excluded so an unauthenticated POST/PUT/DELETE can never ride this rule.
     */
    static boolean isSpaNavigation(HttpServletRequest request) {
        if (!HttpMethod.GET.name().equals(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        for (String prefix : NON_SPA_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtTokenService jwtTokenService(JwtProperties properties) {
        return new JwtTokenService(properties);
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        return new JwtAuthenticationFilter(jwtTokenService);
    }

    /**
     * The staff chain. {@code @Order(2)} because the player-facing chain claims {@code /v1/**} with
     * {@code @Order(1)}: with two chains in the context, the order decides which one sees a request first, and
     * leaving it implicit would make that a framework implementation detail rather than a decision (ADR-0026).
     *
     * <p>Rate limiting is not added here: it is one filter for both surfaces
     * ({@code dev.strata.common.core.ratelimit.RateLimitFilter}, registered before the security chain so a
     * throttled login never reaches a password hash). A second limiter inside this chain would reject the same
     * request twice with two different answers.
     */
    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            List<PermitPathContributor> permitPathContributors,
            ObjectMapper objectMapper)
            throws Exception {
        String[] modulePermitPaths = permitPathContributors.stream()
                .flatMap(contributor -> contributor.permitPaths().stream())
                .distinct()
                .toArray(String[]::new);
        String[] permitPaths = Stream.concat(Arrays.stream(CORE_PERMIT_PATHS), Arrays.stream(modulePermitPaths))
                .distinct()
                .toArray(String[]::new);
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ASYNC)
                        .permitAll()
                        .requestMatchers(permitPaths)
                        .permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        .requestMatchers(SPA_STATIC_PATHS)
                        .permitAll()
                        .requestMatchers(SecurityAutoConfiguration::isSpaNavigation)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(handling -> handling.authenticationEntryPoint((request, response, exception) ->
                                writeProblem(request, response, ErrorCode.UNAUTHORIZED, objectMapper))
                        .accessDeniedHandler((request, response, exception) ->
                                writeProblem(request, response, ErrorCode.FORBIDDEN, objectMapper)))
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        // Browser hardening headers (CSP/HSTS/frame/referrer/permissions), shared with the player chain.
        SecurityHeaders.apply(http);
        return http.build();
    }

    /**
     * Rejections from the filter chain never reach a controller advice, so this is the only place a
     * missing/invalid credential can be recorded. Without the log line a 401 exists only as an HTTP
     * response and cannot be correlated with the request that caused it (docs/05 §日志与排错).
     */
    private static void writeProblem(
            HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode, ObjectMapper objectMapper)
            throws IOException {
        // Observability filters may run after the trace filter and overwrite MDC's traceId with a
        // generated span id, so MDC is not reliable here. The caller-supplied X-Trace-Id header is
        // the authoritative value for both the log line and the Problem Details payload
        // (docs/05 §日志与排错).
        String traceId = request.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        if (traceId == null) {
            traceId = MDC.get(TraceIdFilter.MDC_KEY);
        }
        log.warn(
                "{} {} rejected: {} (traceId={})",
                request.getMethod(),
                request.getRequestURI(),
                errorCode.name(),
                traceId);
        ProblemDetail detail = ProblemDetails.of(errorCode, errorCode.defaultMessage());
        if (traceId != null) {
            detail.setProperty("traceId", traceId);
        }
        response.setStatus(detail.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), detail);
    }
}
