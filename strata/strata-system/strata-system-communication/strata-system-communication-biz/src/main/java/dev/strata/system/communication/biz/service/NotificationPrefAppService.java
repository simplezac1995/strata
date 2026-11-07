// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.service;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.system.api.dto.NotificationPrefDto;
import dev.strata.system.api.request.NotificationPrefUpdateRequest;
import dev.strata.system.communication.api.entity.UserNotificationPref;
import dev.strata.system.communication.biz.mapper.NotificationPrefMapper;
import dev.strata.system.communication.biz.repository.UserNotificationPrefRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPrefAppService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPrefAppService.class);

    private final UserNotificationPrefRepository userNotificationPrefRepository;
    private final NotificationPrefMapper notificationPrefMapper;

    public NotificationPrefAppService(
            UserNotificationPrefRepository userNotificationPrefRepository,
            NotificationPrefMapper notificationPrefMapper) {
        this.userNotificationPrefRepository = userNotificationPrefRepository;
        this.notificationPrefMapper = notificationPrefMapper;
    }

    @Transactional(readOnly = true)
    public NotificationPrefDto getCurrent() {
        return notificationPrefMapper.toDto(requireOrCreate(currentUserId()));
    }

    @Transactional
    public NotificationPrefDto updateCurrent(NotificationPrefUpdateRequest request) {
        UserNotificationPref pref = requireOrCreate(currentUserId());
        if (request.emailEnabled() != null) {
            pref.setEmailEnabled(request.emailEnabled());
        }
        if (request.inAppEnabled() != null) {
            pref.setInAppEnabled(request.inAppEnabled());
        }
        if (request.marketingEnabled() != null) {
            pref.setMarketingEnabled(request.marketingEnabled());
        }
        if (request.securityEnabled() != null) {
            pref.setSecurityEnabled(request.securityEnabled());
        }
        userNotificationPrefRepository.save(pref);
        return notificationPrefMapper.toDto(pref);
    }

    private UserNotificationPref requireOrCreate(Long userId) {
        return userNotificationPrefRepository.findById(userId).orElseGet(() -> {
            UserNotificationPref pref = new UserNotificationPref();
            pref.setUserId(userId);
            return userNotificationPrefRepository.save(pref);
        });
    }

    private static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Not authenticated");
        }
        return principal.userId();
    }
}
