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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthAppServiceAccountTest extends AuthAppServiceAccountTestBase {

    @Nested
    class RegisterTests {

        @Test
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
    class ForgotPasswordTests {

        @Test
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
        void forgotPassword_UserMissing_NoSave() {
            when(userRepository.findByUsernameAndDeletedFalse("ghost")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());

            assertThatCode(() -> authAppService.forgotPassword(new ForgotPasswordRequest("ghost")))
                    .doesNotThrowAnyException();

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    class ResetPasswordTests {

        @Test
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
        void resetPassword_BadToken_Throws() {
            when(userRepository.findByResetToken(DigestUtils.sha256Hex("bad"))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authAppService.resetPassword(new ResetPasswordRequest("bad", "new-password-1")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
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
    class ChangePasswordTests {

        @Test
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
        void changePassword_Missing_Throws() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authAppService.changePassword(99L, new ChangePasswordRequest("old", "new")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
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
