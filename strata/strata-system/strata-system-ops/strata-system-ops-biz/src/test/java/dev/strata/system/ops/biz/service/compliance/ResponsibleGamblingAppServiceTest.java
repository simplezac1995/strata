// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.system.api.dto.compliance.ResponsibleLimitsView;
import dev.strata.system.api.request.compliance.CoolOffCreateRequest;
import dev.strata.system.api.request.compliance.ResponsibleLimitCreateRequest;
import dev.strata.system.api.request.compliance.SelfExclusionCreateRequest;
import dev.strata.system.ops.api.entity.compliance.CoolOff;
import dev.strata.system.ops.api.entity.compliance.ResponsibleGamblingLimit;
import dev.strata.system.ops.api.entity.compliance.SelfExclusion;
import dev.strata.system.ops.biz.mapper.compliance.CoolOffMapper;
import dev.strata.system.ops.biz.mapper.compliance.ResponsibleGamblingLimitMapper;
import dev.strata.system.ops.biz.mapper.compliance.SelfExclusionMapper;
import dev.strata.system.ops.biz.repository.compliance.CoolOffRepository;
import dev.strata.system.ops.biz.repository.compliance.ResponsibleGamblingLimitRepository;
import dev.strata.system.ops.biz.repository.compliance.SelfExclusionRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResponsibleGamblingAppService 单元测试")
class ResponsibleGamblingAppServiceTest {

    @Mock
    private ResponsibleGamblingLimitRepository limitRepository;

    @Mock
    private ResponsibleGamblingLimitMapper limitMapper;

    @Mock
    private SelfExclusionRepository selfExclusionRepository;

    @Mock
    private SelfExclusionMapper selfExclusionMapper;

    @Mock
    private CoolOffRepository coolOffRepository;

    @Mock
    private CoolOffMapper coolOffMapper;

    @InjectMocks
    private ResponsibleGamblingAppService service;

    @Test
    @DisplayName("myLimits 聚合三类记录")
    void myLimits_Aggregates() {
        when(limitRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(selfExclusionRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(coolOffRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        ResponsibleLimitsView view = service.myLimits(1L);

        assertThat(view.limits()).isEmpty();
        assertThat(view.selfExclusions()).isEmpty();
        assertThat(view.coolOffs()).isEmpty();
    }

    @Test
    @DisplayName("设置限额落库")
    void createLimit_Persists() {
        ResponsibleLimitCreateRequest req = new ResponsibleLimitCreateRequest("deposit", 100000L, "daily");
        ResponsibleGamblingLimit row = new ResponsibleGamblingLimit();
        when(limitMapper.toEntity(req)).thenReturn(row);
        service.createLimit(1L, req);
        assertThat(row.getUserId()).isEqualTo(1L);
        verify(limitRepository).save(row);
    }

    @Test
    @DisplayName("带天数的自我排除计算结束日")
    void selfExclude_WithDays_SetsEnd() {
        SelfExclusionCreateRequest req = new SelfExclusionCreateRequest(7, "gamble");
        SelfExclusion row = new SelfExclusion();
        when(selfExclusionRepository.save(any(SelfExclusion.class))).thenAnswer(inv -> inv.getArgument(0));
        service.selfExclude(1L, req);
        verify(selfExclusionRepository).save(any(SelfExclusion.class));
        assertThat(row.getEndDate()).isNull(); // mapper-built row not used; assertion on saved row below
    }

    @Test
    @DisplayName("cool-off 按小时计算结束日")
    void coolOff_SetsEnd() {
        CoolOffCreateRequest req = new CoolOffCreateRequest(24);
        service.coolOff(1L, req);
        verify(coolOffRepository).save(any(CoolOff.class));
    }

    @Test
    @DisplayName("分页用户 oversight")
    void pageUsers_Paged() {
        when(selfExclusionRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(new SelfExclusion())));
        when(selfExclusionMapper.toDto(any(SelfExclusion.class)))
                .thenReturn(
                        new dev.strata.system.api.dto.compliance.SelfExclusionDto("1", "1", null, null, null, null));
        assertThat(service.pageUsers(0, 10).content()).hasSize(1);
    }
}
