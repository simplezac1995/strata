// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.system.api.dto.UserDto;
import dev.strata.system.api.request.ChangePasswordRequest;
import dev.strata.system.api.request.ForgotPasswordRequest;
import dev.strata.system.api.request.RegisterRequest;
import dev.strata.system.api.request.ResetPasswordRequest;
import dev.strata.system.rbac.api.entity.User;
import java.time.Instant;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthAppService 注册与密码")
class AuthAppServiceAccountTest extends AuthAppServiceAccountTestBase {

    @Nested
    @DisplayName("注册 register")
    class RegisterTests {

        @Test
        @DisplayName("注册成功创建租户与用户并返回 DTO")
        void register_Success() {
            RegisterRequest request = new RegisterRequest("alice", "Ali", "a@b.com", "pw123456", "Acme", "acme");
            when(userRepository.existsByUsernameAndDeletedFalse("alice")).thenReturn(false);
            when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
            when(passwordEncoder.encode("pw123456")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toDto(any(User.class), anyList())).thenReturn(userDto());

            UserDto result = authAppService.register(request);

            assertThat(result.username()).isEqualTo("alice");
            org.mockito.ArgumentCaptor<dev.strata.system.rbac.api.entity.Tenant> tenantCaptor =
                    org.mockito.ArgumentCaptor.forClass(dev.strata.system.rbac.api.entity.Tenant.class);
            verify(tenantRepository).save(tenantCaptor.capture());
            assertThat(tenantCaptor.getValue().getCode()).isEqualTo("acme");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("用户名已存在时返回通用冲突且执行虚拟密码校验")
        void register_UsernameExists_Throws() {
            RegisterRequest request = new RegisterRequest("bob", "Bob", null, "pw123456", "Ws", "ws");
            when(userRepository.existsByUsernameAndDeletedFalse("bob")).thenReturn(true);

            assertThatThrownBy(() -> authAppService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Registration could not be completed; please review your details")
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.CONFLICT));

            verify(passwordEncoder).matches(anyString(), anyString());
            verify(tenantRepository, never()).save(any());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("邮箱已存在抛出 CONFLICT")
        void register_EmailExists_Throws() {
            RegisterRequest request = new RegisterRequest("carol", "Carol", "c@x.com", "pw123456", "Ws", "ws");
            when(userRepository.existsByUsernameAndDeletedFalse("carol")).thenReturn(false);
            when(userRepository.existsByEmail("c@x.com")).thenReturn(true);

            assertThatThrownBy(() -> authAppService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Registration could not be completed; please review your details")
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.CONFLICT));

            verify(passwordEncoder).matches(anyString(), anyString());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("忘记密码 forgotPassword")
    class ForgotPasswordTests {

        @Test
        @DisplayName("存在用户时生成重置令牌哈希并落库")
        void forgotPassword_UserFound_StoresTokenHash() {
            User user = enabledUser(1L, "alice");
            when(userRepository.findByUsernameAndDeletedFalse("alice")).thenReturn(Optional.of(user));

            authAppService.forgotPassword(new ForgotPasswordRequest("alice"));

            assertThat(user.getResetToken()).isNotBlank();
            assertThat(user.getResetToken()).isNotEqualTo("alice");
            assertThat(user.getResetTokenExpiry()).isNotNull();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("用户不存在时静默不落库")
        void forgotPassword_UserMissing_NoSave() {
            when(userRepository.findByUsernameAndDeletedFalse("ghost")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());

            assertThatCode(() -> authAppService.forgotPassword(new ForgotPasswordRequest("ghost")))
                    .doesNotThrowAnyException();

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("重置密码 resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("有效未过期令牌可重置密码并清除令牌")
        void resetPassword_ValidToken_Success() {
            User user = enabledUser(1L, "alice");
            user.setResetTokenExpiry(Instant.now().plusSeconds(600));
            when(userRepository.findByResetToken(DigestUtils.sha256Hex("good-token")))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.encode("new-password-1")).thenReturn("new-hash");

            authAppService.resetPassword(new ResetPasswordRequest("good-token", "new-password-1"));

            assertThat(user.getPasswordHash()).isEqualTo("new-hash");
            assertThat(user.getResetToken()).isNull();
            assertThat(user.getResetTokenExpiry()).isNull();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("无效令牌抛出 BAD_REQUEST")
        void resetPassword_BadToken_Throws() {
            when(userRepository.findByResetToken(DigestUtils.sha256Hex("bad"))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authAppService.resetPassword(new ResetPasswordRequest("bad", "new-password-1")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("令牌已过期抛出 BAD_REQUEST")
        void resetPassword_ExpiredToken_Throws() {
            User user = enabledUser(1L, "alice");
            user.setResetTokenExpiry(Instant.now().minusSeconds(60));
            when(userRepository.findByResetToken(DigestUtils.sha256Hex("expired")))
                    .thenReturn(Optional.of(user));

            assertThatThrownBy(
                            () -> authAppService.resetPassword(new ResetPasswordRequest("expired", "new-password-1")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("修改密码 changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("旧密码正确时更新密码")
        void changePassword_CorrectOld_Success() {
            User user = enabledUser(1L, "alice");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("old", "hash")).thenReturn(true);
            when(passwordEncoder.encode("new")).thenReturn("new-hash");

            authAppService.changePassword(1L, new ChangePasswordRequest("old", "new"));

            assertThat(user.getPasswordHash()).isEqualTo("new-hash");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("旧密码错误抛出 BAD_REQUEST")
        void changePassword_WrongOld_Throws() {
            User user = enabledUser(1L, "alice");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

            assertThatThrownBy(() -> authAppService.changePassword(1L, new ChangePasswordRequest("wrong", "new")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("用户不存在抛出 NOT_FOUND")
        void changePassword_Missing_Throws() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authAppService.changePassword(99L, new ChangePasswordRequest("old", "new")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("已删除用户抛出 NOT_FOUND")
        void changePassword_Deleted_Throws() {
            User user = enabledUser(1L, "alice");
            user.setDeleted(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authAppService.changePassword(1L, new ChangePasswordRequest("old", "new")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(userRepository, never()).save(any(User.class));
        }
    }
}
