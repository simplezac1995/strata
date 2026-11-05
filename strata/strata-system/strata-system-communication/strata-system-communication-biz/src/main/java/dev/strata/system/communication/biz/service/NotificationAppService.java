// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.NotificationDto;
import dev.strata.system.api.request.NotificationBroadcastRequest;
import dev.strata.system.communication.api.entity.Notification;
import dev.strata.system.communication.biz.mapper.NotificationMapper;
import dev.strata.system.communication.biz.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Per-staff notification inbox plus the admin broadcast fan-out. */
@Service
public class NotificationAppService {

    private static final Logger log = LoggerFactory.getLogger(NotificationAppService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public NotificationAppService(
            NotificationRepository notificationRepository, NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<NotificationDto> listForUser(Long userId, boolean onlyUnread, int page, int size) {
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Page<Notification> result = onlyUnread
                ? notificationRepository.findByRecipientUserIdAndReadAtIsNull(userId, PageRequests.of(page, size, sort))
                : notificationRepository.findByRecipientUserId(userId, PageRequests.of(page, size, sort));
        return PageResult.from(result.map(notificationMapper::toDto));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByRecipientUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        Notification notification = notificationRepository
                .findByIdAndRecipientUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId, Instant.now());
    }

    @OperationLog(module = "system:notification", action = "create")
    @Transactional
    public int broadcast(NotificationBroadcastRequest request) {
        List<Long> recipientIds = notificationRepository.findActiveStaffUserIds();
        if (recipientIds.isEmpty()) {
            return 0;
        }
        Instant now = Instant.now();
        List<Notification> rows = recipientIds.stream()
                .map(recipientId -> {
                    Notification row = new Notification();
                    row.setRecipientUserId(recipientId);
                    row.setCategory(request.category());
                    row.setTitle(request.title());
                    row.setContent(request.content());
                    row.setActionUrl(request.actionUrl());
                    row.setCreatedAt(now);
                    return row;
                })
                .toList();
        notificationRepository.saveAll(rows);
        return rows.size();
    }
}
