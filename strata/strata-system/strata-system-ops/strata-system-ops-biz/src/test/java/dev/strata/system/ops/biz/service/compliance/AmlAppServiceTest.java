// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.system.api.request.compliance.AmlReportReviewRequest;
import dev.strata.system.api.request.compliance.AmlRuleCreateRequest;
import dev.strata.system.ops.api.entity.compliance.AmlReport;
import dev.strata.system.ops.api.entity.compliance.AmlRule;
import dev.strata.system.ops.biz.mapper.compliance.AmlReportMapper;
import dev.strata.system.ops.biz.mapper.compliance.AmlRuleMapper;
import dev.strata.system.ops.biz.repository.compliance.AmlReportRepository;
import dev.strata.system.ops.biz.repository.compliance.AmlRuleRepository;
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
@DisplayName("AmlAppService 单元测试")
@SuppressWarnings({"unchecked", "varargs"})
class AmlAppServiceTest {

    @Mock
    private AmlRuleRepository amlRuleRepository;

    @Mock
    private AmlRuleMapper amlRuleMapper;

    @Mock
    private AmlReportRepository amlReportRepository;

    @Mock
    private AmlReportMapper amlReportMapper;

    @InjectMocks
    private AmlAppService service;

    @Test
    @DisplayName("创建规则落库")
    void createRule_Persists() {
        AmlRuleCreateRequest req = new AmlRuleCreateRequest("Big deposit", "large_deposit", 1000000L, "report", true);
        AmlRule row = new AmlRule();
        when(amlRuleMapper.toEntity(req)).thenReturn(row);
        when(amlRuleRepository.save(row)).thenReturn(row);
        service.createRule(req);
        verify(amlRuleRepository).save(row);
    }

    @Test
    @DisplayName("更新规则存在时更新")
    void updateRule_Existing_Updates() {
        AmlRule row = new AmlRule();
        when(amlRuleRepository.findById(5L)).thenReturn(Optional.of(row));
        when(amlRuleRepository.save(row)).thenReturn(row);
        var req = new dev.strata.system.api.request.compliance.AmlRuleUpdateRequest(
                "Big", "velocity", 10L, "report", true);
        service.updateRule(5L, req);
        verify(amlRuleMapper).update(req, row);
    }

    @Test
    @DisplayName("删除规则存在时删除")
    void deleteRule_Existing_Deletes() {
        AmlRule row = new AmlRule();
        when(amlRuleRepository.findById(5L)).thenReturn(Optional.of(row));
        service.deleteRule(5L);
        verify(amlRuleRepository).delete(row);
    }

    @Test
    @DisplayName("删除不存在抛 NOT_FOUND")
    void deleteRule_Missing_Throws() {
        when(amlRuleRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteRule(9L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("分页规则")
    void pageRules_Paged() {
        when(amlRuleRepository.findAll(
                        any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new AmlRule())));
        when(amlRuleMapper.toDto(any(AmlRule.class)))
                .thenReturn(
                        new dev.strata.system.api.dto.compliance.AmlRuleDto("1", "n", "t", 1L, "report", true, null));
        assertThat(service.pageRules(null, null, 0, 10).content()).hasSize(1);
    }

    @Test
    @DisplayName("分页报告")
    void pageReports_Paged() {
        when(amlReportRepository.findAll(
                        any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new AmlReport())));
        when(amlReportMapper.toDto(any(AmlReport.class)))
                .thenReturn(new dev.strata.system.api.dto.compliance.AmlReportDto(
                        "1", "1", "t", 1L, null, "pending", null));
        assertThat(service.pageReports(null, 0, 10).content()).hasSize(1);
    }

    @Test
    @DisplayName("复核报告更新状态")
    void reviewReport_UpdatesStatus() {
        AmlReport row = new AmlReport();
        when(amlReportRepository.findById(5L)).thenReturn(Optional.of(row));
        when(amlReportRepository.save(row)).thenReturn(row);
        service.reviewReport(5L, new AmlReportReviewRequest("cleared", "ok"));
        assertThat(row.getStatus()).isEqualTo("cleared");
    }

    @Test
    @DisplayName("复核不存在抛 NOT_FOUND")
    void reviewReport_Missing_Throws() {
        when(amlReportRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reviewReport(9L, new AmlReportReviewRequest("cleared", null)))
                .isInstanceOf(BusinessException.class);
    }
}
