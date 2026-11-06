// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.system.api.dto.SignInConfigDto;
import dev.strata.system.api.dto.SignInRecordDto;
import dev.strata.system.api.request.SignInConfigUpsertRequest;
import dev.strata.system.ops.api.entity.SignInConfig;
import dev.strata.system.ops.api.entity.SignInRecord;
import dev.strata.system.ops.biz.mapper.SignInConfigMapper;
import dev.strata.system.ops.biz.mapper.SignInRecordMapper;
import dev.strata.system.ops.biz.repository.SignInConfigRepository;
import dev.strata.system.ops.biz.repository.SignInRecordRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignInAppService {

    private final SignInConfigRepository signInConfigRepository;
    private final SignInRecordRepository signInRecordRepository;
    private final SignInConfigMapper signInConfigMapper;
    private final SignInRecordMapper signInRecordMapper;

    public SignInAppService(
            SignInConfigRepository signInConfigRepository,
            SignInRecordRepository signInRecordRepository,
            SignInConfigMapper signInConfigMapper,
            SignInRecordMapper signInRecordMapper) {
        this.signInConfigRepository = signInConfigRepository;
        this.signInRecordRepository = signInRecordRepository;
        this.signInConfigMapper = signInConfigMapper;
        this.signInRecordMapper = signInRecordMapper;
    }

    @Transactional(readOnly = true)
    public List<SignInConfigDto> getConfig() {
        return signInConfigRepository.findAllByOrderByDayAsc().stream()
                .map(signInConfigMapper::toDto)
                .toList();
    }

    /** Full replacement of the per-day reward table. */
    @Transactional
    @OperationLog(module = "ops:signin", action = "update")
    public List<SignInConfigDto> replaceConfig(SignInConfigUpsertRequest request) {
        signInConfigRepository.deleteAll();
        List<SignInConfig> configs =
                request.items().stream().map(signInConfigMapper::toEntity).toList();
        signInConfigRepository.saveAll(configs);
        return signInConfigRepository.findAllByOrderByDayAsc().stream()
                .map(signInConfigMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<SignInRecordDto> records(Long userId, int page, int size) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "userId is required");
        }
        return PageResult.from(signInRecordRepository
                .findByUserIdOrderBySignDateDescIdDesc(
                        userId,
                        PageRequests.of(page, size, Sort.by("signDate", "id").descending()))
                .map(signInRecordMapper::toDto));
    }

    /**
     * Current player signs in for today. TODO: wire real reward grant / continuous-cycle reset; this
     * stub records the day reward from config.
     */
    @Transactional
    @OperationLog(module = "ops:signin", action = "create")
    public SignInRecordDto checkin() {
        Long userId = currentUserId();
        LocalDate today = LocalDate.now();
        if (signInRecordRepository.existsByUserIdAndSignDate(userId, today)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Already signed in today");
        }
        long prior = signInRecordRepository.countByUserId(userId);
        int dayNumber = (int) prior + 1;
        SignInConfig config = signInConfigRepository.findByDay(dayNumber);

        SignInRecord record = new SignInRecord();
        record.setUserId(userId);
        record.setSignDate(today);
        record.setDayNumber(dayNumber);
        if (config != null) {
            record.setRewardPoints(config.getRewardPoints());
            record.setRewardMinor(config.getRewardMinor());
        } else {
            record.setRewardPoints(0L);
            record.setRewardMinor(java.math.BigDecimal.ZERO);
        }
        signInRecordRepository.save(record);
        return signInRecordMapper.toDto(record);
    }

    private static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Not authenticated");
        }
        return principal.userId();
    }
}
