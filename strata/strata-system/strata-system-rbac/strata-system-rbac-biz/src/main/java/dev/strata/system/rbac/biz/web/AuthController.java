// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.common.security.ratelimit.RateLimit;
import dev.strata.system.api.dto.CaptchaIssueResponse;
import dev.strata.system.api.dto.TokenResponse;
import dev.strata.system.api.dto.TotpEnrollResponse;
import dev.strata.system.api.dto.UserDto;
import dev.strata.system.api.request.ChangePasswordRequest;
import dev.strata.system.api.request.ForgotPasswordRequest;
import dev.strata.system.api.request.LoginRequest;
import dev.strata.system.api.request.MfaVerifyRequest;
import dev.strata.system.api.request.RefreshTokenRequest;
import dev.strata.system.api.request.RegisterRequest;
import dev.strata.system.api.request.ResetPasswordRequest;
import dev.strata.system.api.request.TotpConfirmRequest;
import dev.strata.system.api.request.TotpDisableRequest;
import dev.strata.system.api.request.VerifyEmailRequest;
import dev.strata.system.rbac.biz.security.AdminCaptchaService;
import dev.strata.system.rbac.biz.service.AuthAppService;
import dev.strata.system.rbac.biz.service.SocialAuthorizeResponse;
import dev.strata.system.rbac.biz.service.SocialLoginAppService;
import dev.strata.system.rbac.biz.service.TotpAppService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/system/auth")
public class AuthController {

    private final AuthAppService authAppService;
    private final AdminCaptchaService adminCaptchaService;
    private final TotpAppService totpAppService;
    private final SocialLoginAppService socialLoginAppService;

    public AuthController(
            AuthAppService authAppService,
            AdminCaptchaService adminCaptchaService,
            TotpAppService totpAppService,
            SocialLoginAppService socialLoginAppService) {
        this.authAppService = authAppService;
        this.adminCaptchaService = adminCaptchaService;
        this.totpAppService = totpAppService;
        this.socialLoginAppService = socialLoginAppService;
    }

    /**
     * Issues a math captcha challenge. Public and rate-limited: the login page calls this once the server signals
     * a captcha is required (or proactively to pre-warm it).
     */
    @RateLimit(limit = 20, window = 60, key = "auth_captcha")
    @GetMapping("/captcha")
    public Result<CaptchaIssueResponse> captcha() {
        return Result.ok(adminCaptchaService.issue());
    }

    @RateLimit(limit = 5, window = 60, key = "auth_login")
    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.ok(
                authAppService.login(request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent")));
    }

    @RateLimit(limit = 30, window = 60, key = "auth_refresh")
    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.ok(authAppService.refresh(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authAppService.logout(request);
        return Result.ok();
    }

    @RateLimit(limit = 10, window = 60, key = "auth_register")
    @PostMapping("/register")
    public Result<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authAppService.register(request));
    }

    @RateLimit(limit = 5, window = 60, key = "auth_forgot_password")
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authAppService.forgotPassword(request);
        return Result.ok();
    }

    @RateLimit(limit = 5, window = 60, key = "auth_reset_password")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authAppService.resetPassword(request);
        return Result.ok();
    }

    @RateLimit(limit = 10, window = 60, key = "auth_change_password")
    @PostMapping("/change-password")
    public Result<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        authAppService.changePassword(principal.userId(), request);
        return Result.ok();
    }

    @PostMapping("/verify-email")
    public Result<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authAppService.verifyEmail(request);
        return Result.ok();
    }

    /**
     * Starts TOTP enrollment for the authenticated admin: returns a fresh secret, the otpauth URI and a QR
     * data URL. MFA stays off until the admin confirms a valid code via {@link #confirmMfa}.
     */
    @PostMapping("/mfa/enroll")
    public Result<TotpEnrollResponse> enrollMfa(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return Result.ok(totpAppService.enroll(principal.userId()));
    }

    /** Confirms a pending enrollment with a valid current TOTP code, flipping the second factor on. */
    @PostMapping("/mfa/enroll/confirm")
    public Result<Void> confirmMfa(@Valid @RequestBody TotpConfirmRequest request, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        totpAppService.confirmEnrollment(principal.userId(), request.code());
        return Result.ok();
    }

    /**
     * Completes an MFA-gated login. Public (no session yet) and rate-limited: exchanges the challenge token
     * issued by {@link #login} plus a current TOTP code for a real session.
     */
    @RateLimit(limit = 10, window = 60, key = "auth_mfa_verify")
    @PostMapping("/mfa/verify")
    public Result<TokenResponse> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest request, HttpServletRequest httpRequest) {
        return Result.ok(
                authAppService.mfaVerify(request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent")));
    }

    /** Self-service MFA disable: requires a current TOTP code to prove possession. */
    @PostMapping("/mfa/disable")
    public Result<Void> disableMfa(@Valid @RequestBody TotpDisableRequest request, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        totpAppService.disable(principal.userId(), request.code());
        return Result.ok();
    }

    /**
     * Social-login authorize endpoint (P2-9 skeleton). Public and rate-limited: validates {@code redirect_uri}
     * against the configured whitelist, mints a one-time state + PKCE verifier server-side, and returns the
     * state to echo plus the provider authorize URL. The flow is disabled by default until a provider and its
     * client credentials are configured.
     */
    @RateLimit(limit = 10, window = 60, key = "auth_social")
    @GetMapping("/social/authorize")
    public Result<SocialAuthorizeResponse> socialAuthorize(
            @RequestParam String provider, @RequestParam String redirectUri) {
        return Result.ok(socialLoginAppService.begin(provider, redirectUri));
    }
}
