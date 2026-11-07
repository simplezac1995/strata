// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.system.api.dto.NotificationDto;
import dev.strata.system.api.request.NotificationBroadcastRequest;
import dev.strata.system.communication.api.entity.Notification;
import dev.strata.system.communication.biz.mapper.NotificationMapper;
import dev.strata.system.communication.biz.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificationAppServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationAppService service;

    private NotificationDto dto() {
        return new NotificationDto("1", "100", "system", "t", "b", null, null, Instant.now());
    }

    @Test
    void list_OnlyUnread() {
        when(notificationRepository.findByRecipientUserIdAndReadAtIsNull(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new Notification())));
        when(notificationMapper.toDto(any(Notification.class))).thenReturn(dto());

        assertThat(service.listForUser(100L, true, 0, 10).content()).hasSize(1);
    }

    @Test
    void unreadCount_ReturnsCount() {
        when(notificationRepository.countByRecipientUserIdAndReadAtIsNull(100L)).thenReturn(3L);
        assertThat(service.unreadCount(100L)).isEqualTo(3L);
    }

    @Test
    void markRead_SetsReadAt() {
        Notification n = new Notification();
        when(notificationRepository.findByIdAndRecipientUserId(1L, 100L)).thenReturn(Optional.of(n));

        service.markRead(100L, 1L);

        assertThat(n.getReadAt()).isNotNull();
        verify(notificationRepository).save(n);
    }

    @Test
    void markRead_NotFound_Throws() {
        when(notificationRepository.findByIdAndRecipientUserId(99L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(100L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void markAllRead_Delegates() {
        service.markAllRead(100L);
        verify(notificationRepository).markAllRead(eq(100L), any(Instant.class));
    }

    @Test
    void broadcast_FansOut() {
        when(notificationRepository.findActiveStaffUserIds()).thenReturn(List.of(1L, 2L, 3L));
        NotificationBroadcastRequest req = new NotificationBroadcastRequest("system", "title", "body", "/home");

        int sent = service.broadcast(req);

        assertThat(sent).isEqualTo(3);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
        assertThat(captor.getValue().get(0).getCategory()).isEqualTo("system");
    }
}
