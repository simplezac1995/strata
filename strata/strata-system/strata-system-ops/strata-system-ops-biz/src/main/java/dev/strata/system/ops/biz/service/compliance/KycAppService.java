// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service.compliance;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.data.spec.Specs;
import dev.strata.system.api.dto.compliance.KycApplicationDto;
import dev.strata.system.api.request.compliance.KycReviewRequest;
import dev.strata.system.api.request.compliance.KycSubmitRequest;
import dev.strata.system.ops.api.entity.compliance.KycApplication;
import dev.strata.system.ops.biz.mapper.compliance.KycApplicationMapper;
import dev.strata.system.ops.biz.repository.compliance.KycApplicationRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** KYC real-name verification (framework; OCR ingestion is TODO). */
@Service
public class KycAppService {

    private static final Logger log = LoggerFactory.getLogger(KycAppService.class);

    private final KycApplicationRepository kycApplicationRepository;
    private final KycApplicationMapper kycApplicationMapper;

    public KycAppService(KycApplicationRepository kycApplicationRepository, KycApplicationMapper kycApplicationMapper) {
        this.kycApplicationRepository = kycApplicationRepository;
        this.kycApplicationMapper = kycApplicationMapper;
    }

    @Transactional
    @OperationLog(module = "compliance:kyc", action = "create")
    public KycApplicationDto submit(Long userId, KycSubmitRequest request) {
        KycApplication row = kycApplicationMapper.toEntity(request);
        row.setUserId(userId);
        row.setStatus("pending");
        row.setSubmittedAt(Instant.now());
        kycApplicationRepository.save(row);
        return kycApplicationMapper.toDto(row);
    }

    @Transactional(readOnly = true)
    public KycApplicationDto myStatus(Long userId) {
        return kycApplicationRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(kycApplicationMapper::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResult<KycApplicationDto> page(String status, int page, int size) {
        var spec = Specs.of(KycApplication.class).eq("status", status).toSpecification();
        Page<KycApplication> result = kycApplicationRepository.findAll(
                spec, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.from(result.map(kycApplicationMapper::toDto));
    }

    @Transactional
    @OperationLog(module = "compliance:kyc", action = "update")
    public KycApplicationDto review(Long id, Long reviewerId, KycReviewRequest request) {
        KycApplication row = kycApplicationRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("KYC application not found"));
        if ("approved".equals(row.getStatus()) || "rejected".equals(row.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "KYC application already reviewed");
        }
        row.setReviewerId(reviewerId);
        row.setReviewedAt(Instant.now());
        if (request.approve()) {
            row.setStatus("approved");
            row.setRejectReason(null);
        } else {
            row.setStatus("rejected");
            row.setRejectReason(request.rejectReason());
        }
        kycApplicationRepository.save(row);
        return kycApplicationMapper.toDto(row);
    }
}
