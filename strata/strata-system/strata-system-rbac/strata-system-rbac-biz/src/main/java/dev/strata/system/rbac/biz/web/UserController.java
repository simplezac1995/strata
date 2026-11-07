// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.system.api.dto.UserDto;
import dev.strata.system.api.request.UpdateMyAvatarRequest;
import dev.strata.system.api.request.UpdateMyEmailRequest;
import dev.strata.system.api.request.UpdateMyPhoneRequest;
import dev.strata.system.api.request.UserBatchDeleteRequest;
import dev.strata.system.api.request.UserCreateRequest;
import dev.strata.system.api.request.UserUpdateRequest;
import dev.strata.system.rbac.biz.service.TotpAppService;
import dev.strata.system.rbac.biz.service.UserAppService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/system/users")
public class UserController {

    private final UserAppService userAppService;
    private final TotpAppService totpAppService;

    public UserController(UserAppService userAppService, TotpAppService totpAppService) {
        this.userAppService = userAppService;
        this.totpAppService = totpAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_USER_LIST)
    public Result<List<UserDto>> list() {
        return Result.ok(userAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.SYSTEM_USER_LIST)
    public Result<PageResult<UserDto>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        return Result.ok(userAppService.page(keyword, page, size, sortBy, sortDir));
    }

    @GetMapping("/export")
    @PreAuthorize(Permissions.SYSTEM_USER_LIST)
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String keyword) {
        byte[] body = userAppService.exportXlsx(keyword);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.xlsx")
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_USER_LIST)
    public Result<UserDto> get(@PathVariable Long id) {
        return Result.ok(userAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_USER_CREATE)
    public Result<UserDto> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.ok(userAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_USER_UPDATE)
    public Result<UserDto> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return Result.ok(userAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_USER_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        userAppService.delete(id);
        return Result.ok();
    }

    @PostMapping("/batch-delete")
    @PreAuthorize(Permissions.SYSTEM_USER_DELETE)
    public Result<Void> batchDelete(@Valid @RequestBody UserBatchDeleteRequest request) {
        userAppService.deleteBatch(request.ids());
        return Result.ok();
    }

    /**
     * GDPR right to erasure: pseudonymizes a user in place (username/email rewritten, PII scrubbed, account
     * disabled) while retaining the row so transaction history stays intact. Guarded by the erasure permission.
     */
    @PostMapping("/{id}/anonymize")
    @PreAuthorize(Permissions.SYSTEM_USER_DELETE)
    public Result<Void> anonymize(@PathVariable Long id) {
        userAppService.anonymize(id);
        return Result.ok();
    }

    /**
     * Administrator reset of a locked-out user's TOTP: wipes the stored secret and disables the second factor
     * without that user's code. Guarded by the user-management permission.
     */
    @PostMapping("/{id}/mfa/reset")
    @PreAuthorize(Permissions.SYSTEM_USER_UPDATE)
    public Result<Void> resetMfa(@PathVariable Long id) {
        totpAppService.reset(id);
        return Result.ok();
    }

    // === Self-service profile (any authenticated staff member edits only their own row) ===

    @PutMapping("/me/email")
    public Result<UserDto> updateMyEmail(
            @Valid @RequestBody UpdateMyEmailRequest request, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return Result.ok(userAppService.updateMyEmail(principal.userId(), request.email()));
    }

    @PutMapping("/me/phone")
    public Result<UserDto> updateMyPhone(
            @Valid @RequestBody UpdateMyPhoneRequest request, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return Result.ok(userAppService.updateMyPhone(principal.userId(), request.phone()));
    }

    @PutMapping("/me/avatar")
    public Result<UserDto> updateMyAvatar(
            @Valid @RequestBody UpdateMyAvatarRequest request, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return Result.ok(userAppService.updateMyAvatar(principal.userId(), request.avatar()));
    }
}
