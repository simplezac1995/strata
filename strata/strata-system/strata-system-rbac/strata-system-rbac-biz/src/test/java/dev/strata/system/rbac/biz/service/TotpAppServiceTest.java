// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.security.crypto.FieldEncryptor;
import dev.strata.system.api.dto.TotpEnrollResponse;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.biz.repository.UserRepository;
import dev.strata.system.rbac.biz.security.QrCodeGenerator;
import dev.strata.system.rbac.biz.security.TotpService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TotpAppServiceTest {

    /** 32 zero bytes, Base64 — a fixed key so encrypt/decrypt round-trips deterministically in the test. */
    private static final String TEST_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @Mock
    private UserRepository userRepository;

    @Mock
    private QrCodeGenerator qrCodeGenerator;

    private TotpService totpService;
    private FieldEncryptor fieldEncryptor;
    private TotpAppService service;

    @BeforeEach
    void setUp() {
        totpService = new TotpService();
        fieldEncryptor = new FieldEncryptor(TEST_KEY);
        service = new TotpAppService(userRepository, totpService, qrCodeGenerator, fieldEncryptor, "Strata Admin");
    }

    private User pendingUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setDeleted(false);
        user.setTotpEnabled(false);
        return user;
    }

    @Test
    void enrollGeneratesPendingSecretStoredEncrypted() {
        User user = pendingUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(qrCodeGenerator.toDataUrl(anyString())).thenReturn("data:image/png;base64,QR");

        TotpEnrollResponse response = service.enroll(1L);

        assertThat(response.secret()).isNotBlank();
        assertThat(response.otpAuthUri()).startsWith("otpauth://totp/");
        assertThat(response.qrCodeDataUrl()).startsWith("data:image/png;base64,");
        assertThat(response.mfaEnabled()).isFalse();
        // Stored value must be the encrypted blob, never the plaintext Base32 secret.
        assertThat(user.getTotpSecret()).isNotEqualTo(response.secret());
        assertThat(user.getTotpSecret()).isNotBlank();
        assertThat(user.isTotpEnabled()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void enrollRejectedWhenAlreadyEnabled() {
        User user = pendingUser();
        user.setTotpEnabled(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.enroll(1L)).isInstanceOf(BusinessException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void confirmEnrollmentHappyPath() {
        User user = pendingUser();
        String secret = totpService.generateSecret();
        user.setTotpSecret(fieldEncryptor.encrypt(secret));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        String code = currentCode(secret);

        service.confirmEnrollment(1L, code);

        assertThat(user.isTotpEnabled()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void confirmEnrollmentRejectsWrongCode() {
        User user = pendingUser();
        String secret = totpService.generateSecret();
        user.setTotpSecret(fieldEncryptor.encrypt(secret));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(
                        () -> service.confirmEnrollment(1L, "000000".equals(currentCode(secret)) ? "111111" : "000000"))
                .isInstanceOf(BusinessException.class);
        assertThat(user.isTotpEnabled()).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void confirmEnrollmentRejectedWithoutPending() {
        User user = pendingUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.confirmEnrollment(1L, "123456")).isInstanceOf(BusinessException.class);
    }

    @Test
    void disableHappyPath() {
        User user = pendingUser();
        String secret = totpService.generateSecret();
        user.setTotpSecret(fieldEncryptor.encrypt(secret));
        user.setTotpEnabled(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.disable(1L, currentCode(secret));

        assertThat(user.getTotpSecret()).isNull();
        assertThat(user.isTotpEnabled()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void disableRejectedWhenNotEnrolled() {
        User user = pendingUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.disable(1L, "123456")).isInstanceOf(BusinessException.class);
    }

    @Test
    void resetClearsTarget() {
        User user = pendingUser();
        user.setTotpSecret(fieldEncryptor.encrypt(totpService.generateSecret()));
        user.setTotpEnabled(true);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));

        service.reset(9L);

        assertThat(user.getTotpSecret()).isNull();
        assertThat(user.isTotpEnabled()).isFalse();
        verify(userRepository).save(user);
    }

    private String currentCode(String secret) {
        long counter = Instant.now().getEpochSecond() / 30;
        try {
            java.lang.reflect.Method decode = TotpService.class.getDeclaredMethod("base32Decode", String.class);
            decode.setAccessible(true);
            java.lang.reflect.Method generate =
                    TotpService.class.getDeclaredMethod("generateCode", byte[].class, long.class);
            generate.setAccessible(true);
            byte[] key = (byte[]) decode.invoke(null, secret);
            return (String) generate.invoke(null, key, counter);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
