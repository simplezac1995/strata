// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.compliance.KycApplicationDto;
import dev.strata.system.api.request.compliance.KycReviewRequest;
import dev.strata.system.api.request.compliance.KycSubmitRequest;
import dev.strata.system.ops.api.entity.compliance.KycApplication;
import dev.strata.system.ops.biz.mapper.compliance.KycApplicationMapper;
import dev.strata.system.ops.biz.repository.compliance.KycApplicationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("KycAppService 单元测试")
@SuppressWarnings({"unchecked", "varargs"})
class KycAppServiceTest {

    @Mock
    private KycApplicationRepository kycApplicationRepository;

    @Mock
    private KycApplicationMapper kycApplicationMapper;

    @InjectMocks
    private KycAppService service;

    @Test
    @DisplayName("提交申请时落库并返回 DTO")
    void submit_PersistsPending() {
        KycSubmitRequest req = new KycSubmitRequest("Alice", "id_card", "hash", "f", "b", "s");
        KycApplication row = new KycApplication();
        when(kycApplicationMapper.toEntity(req)).thenReturn(row);
        when(kycApplicationRepository.save(any(KycApplication.class))).thenReturn(row);
        when(kycApplicationMapper.toDto(row))
                .thenReturn(new KycApplicationDto(
                        "1", "1", "Alice", "id_card", "hash", null, null, null, "pending", null, null, null, null,
                        null));

        KycApplicationDto dto = service.submit(100L, req);

        assertThat(dto.status()).isEqualTo("pending");
        verify(kycApplicationRepository).save(row);
    }

    @Test
    @DisplayName("myStatus 返回最新申请")
    void myStatus_ReturnsLatest() {
        KycApplication row = new KycApplication();
        when(kycApplicationRepository.findFirstByUserIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(row));
        when(kycApplicationMapper.toDto(row))
                .thenReturn(new KycApplicationDto(
                        "1", "1", null, null, null, null, null, null, "pending", null, null, null, null, null));

        assertThat(service.myStatus(100L)).isNotNull();
    }

    @Test
    @DisplayName("分页按状态过滤")
    void page_FiltersByStatus() {
        when(kycApplicationRepository.findAll(
                        any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new KycApplication())));
        when(kycApplicationMapper.toDto(any(KycApplication.class)))
                .thenReturn(new KycApplicationDto(
                        "1", "1", null, null, null, null, null, null, "pending", null, null, null, null, null));

        PageResult<KycApplicationDto> result = service.page("pending", 0, 10);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("审核通过时置 approved 并清空驳回原因")
    void review_Approve_SetsApproved() {
        KycApplication row = new KycApplication();
        row.setStatus("pending");
        when(kycApplicationRepository.findById(5L)).thenReturn(Optional.of(row));
        when(kycApplicationRepository.save(row)).thenReturn(row);
        when(kycApplicationMapper.toDto(row))
                .thenReturn(new KycApplicationDto(
                        "5", "1", null, null, null, null, null, null, "approved", "1", null, null, null, null));

        service.review(5L, 1L, new KycReviewRequest(true, null));

        assertThat(row.getStatus()).isEqualTo("approved");
        verify(kycApplicationRepository).save(row);
    }

    @Test
    @DisplayName("审核驳回时写入原因")
    void review_Reject_SetsReason() {
        KycApplication row = new KycApplication();
        row.setStatus("pending");
        when(kycApplicationRepository.findById(5L)).thenReturn(Optional.of(row));
        when(kycApplicationRepository.save(row)).thenReturn(row);
        when(kycApplicationMapper.toDto(row))
                .thenReturn(new KycApplicationDto(
                        "5", "1", null, null, null, null, null, null, "rejected", "1", "blurry", null, null, null));

        service.review(5L, 1L, new KycReviewRequest(false, "blurry"));

        assertThat(row.getStatus()).isEqualTo("rejected");
        assertThat(row.getRejectReason()).isEqualTo("blurry");
    }

    @Test
    @DisplayName("重复审核抛状态迁移异常")
    void review_AlreadyReviewed_Throws() {
        KycApplication row = new KycApplication();
        row.setStatus("approved");
        when(kycApplicationRepository.findById(5L)).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.review(5L, 1L, new KycReviewRequest(true, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("审核不存在抛 NOT_FOUND")
    void review_Missing_ThrowsNotFound() {
        when(kycApplicationRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.review(9L, 1L, new KycReviewRequest(true, null)))
                .isInstanceOf(BusinessException.class);
    }
}
