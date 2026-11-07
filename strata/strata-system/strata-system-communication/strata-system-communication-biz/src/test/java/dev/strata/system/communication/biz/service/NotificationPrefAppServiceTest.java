// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.system.api.dto.NotificationPrefDto;
import dev.strata.system.api.request.NotificationPrefUpdateRequest;
import dev.strata.system.communication.api.entity.UserNotificationPref;
import dev.strata.system.communication.biz.mapper.NotificationPrefMapper;
import dev.strata.system.communication.biz.repository.UserNotificationPrefRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class NotificationPrefAppServiceTest {

    @Mock
    private UserNotificationPrefRepository userNotificationPrefRepository;

    @Mock
    private NotificationPrefMapper notificationPrefMapper;

    @Mock
    private Authentication authentication;

    private NotificationPrefAppService service;

    @BeforeEach
    void setUp() {
        service = new NotificationPrefAppService(userNotificationPrefRepository, notificationPrefMapper);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(long userId) {
        when(authentication.getPrincipal()).thenReturn(new JwtPrincipal(userId, "alice"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private NotificationPrefDto dto() {
        return new NotificationPrefDto(true, true, false, true);
    }

    @Nested
    class GetCurrentTests {

        @Test
        void getCurrent_Existing_ReturnsDto() {
            authenticate(7L);
            UserNotificationPref pref = new UserNotificationPref();
            pref.setUserId(7L);
            when(userNotificationPrefRepository.findById(7L)).thenReturn(Optional.of(pref));
            when(notificationPrefMapper.toDto(pref)).thenReturn(dto());

            NotificationPrefDto result = service.getCurrent();

            assertThat(result.emailEnabled()).isTrue();
        }

        @Test
        void getCurrent_Missing_CreatesAndSaves() {
            authenticate(7L);
            when(userNotificationPrefRepository.findById(7L)).thenReturn(Optional.empty());
            when(userNotificationPrefRepository.save(any(UserNotificationPref.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(notificationPrefMapper.toDto(any(UserNotificationPref.class))).thenReturn(dto());

            service.getCurrent();

            verify(userNotificationPrefRepository).save(any(UserNotificationPref.class));
        }

        @Test
        void getCurrent_NotAuthenticated_Throws() {
            SecurityContextHolder.getContext().setAuthentication(null);

            assertThatThrownBy(() -> service.getCurrent())
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        }
    }

    @Nested
    class UpdateCurrentTests {

        @Test
        void updateCurrent_AppliesNonNullFields() {
            authenticate(7L);
            UserNotificationPref pref = new UserNotificationPref();
            pref.setUserId(7L);
            when(userNotificationPrefRepository.findById(7L)).thenReturn(Optional.of(pref));
            when(userNotificationPrefRepository.save(pref)).thenReturn(pref);
            when(notificationPrefMapper.toDto(pref)).thenReturn(dto());

            service.updateCurrent(new NotificationPrefUpdateRequest(false, null, true, null));

            assertThat(pref.isEmailEnabled()).isFalse();
            assertThat(pref.isMarketingEnabled()).isTrue();
            verify(userNotificationPrefRepository).save(pref);
        }

        @Test
        void updateCurrent_NotAuthenticated_Throws() {
            SecurityContextHolder.getContext().setAuthentication(null);

            assertThatThrownBy(() -> service.updateCurrent(new NotificationPrefUpdateRequest(true, null, null, null)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));

            verify(userNotificationPrefRepository, never()).save(any(UserNotificationPref.class));
        }
    }
}
