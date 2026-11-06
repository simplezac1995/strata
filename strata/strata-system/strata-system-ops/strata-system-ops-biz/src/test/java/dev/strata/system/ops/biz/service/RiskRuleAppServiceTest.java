// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.RiskRuleDto;
import dev.strata.system.api.request.RiskRuleCreateRequest;
import dev.strata.system.api.request.RiskRuleUpdateRequest;
import dev.strata.system.ops.api.entity.RiskRule;
import dev.strata.system.ops.biz.mapper.RiskRuleMapper;
import dev.strata.system.ops.biz.repository.RiskRuleRepository;
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
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskRuleAppService 单元测试")
@SuppressWarnings({"unchecked", "varargs"})
class RiskRuleAppServiceTest {

    @Mock
    private RiskRuleRepository riskRuleRepository;

    @Mock
    private RiskRuleMapper riskRuleMapper;

    @InjectMocks
    private RiskRuleAppService riskRuleAppService;

    @Test
    @DisplayName("分页查询规则并映射 DTO")
    void page_ReturnsPagedDtos() {
        RiskRule rule = new RiskRule();
        when(riskRuleRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(rule)));
        when(riskRuleMapper.toDto(any(RiskRule.class)))
                .thenReturn(new RiskRuleDto("1", "r", "amount", 1, "reject", true, null));

        PageResult<RiskRuleDto> result = riskRuleAppService.page("amount", true, 0, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("按 id 查询规则存在时返回 DTO")
    void get_Exists_ReturnsDto() {
        RiskRule rule = new RiskRule();
        when(riskRuleRepository.findById(5L)).thenReturn(Optional.of(rule));
        when(riskRuleMapper.toDto(any(RiskRule.class)))
                .thenReturn(new RiskRuleDto("5", "r", "device", 2, "alert", false, null));

        RiskRuleDto dto = riskRuleAppService.get(5L);

        assertThat(dto.id()).isEqualTo("5");
    }

    @Test
    @DisplayName("按 id 查询规则不存在时抛 NOT_FOUND")
    void get_Missing_ThrowsNotFound() {
        when(riskRuleRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskRuleAppService.get(9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Risk rule not found");
    }

    @Test
    @DisplayName("创建规则保存并返回 DTO")
    void create_PersistsAndReturnsDto() {
        RiskRuleCreateRequest req = new RiskRuleCreateRequest("r", "amount", 1, "reject", true);
        RiskRule rule = new RiskRule();
        when(riskRuleMapper.toEntity(req)).thenReturn(rule);
        when(riskRuleRepository.save(rule)).thenReturn(rule);
        when(riskRuleMapper.toDto(rule)).thenReturn(new RiskRuleDto("1", "r", "amount", 1, "reject", true, null));

        RiskRuleDto dto = riskRuleAppService.create(req);

        assertThat(dto.name()).isEqualTo("r");
        verify(riskRuleRepository).save(rule);
    }

    @Test
    @DisplayName("更新规则合并请求并保存")
    void update_MergesAndSaves() {
        RiskRuleUpdateRequest req = new RiskRuleUpdateRequest("r2", "device", 3, "freeze", false);
        RiskRule rule = new RiskRule();
        when(riskRuleRepository.findById(5L)).thenReturn(Optional.of(rule));
        when(riskRuleMapper.toDto(rule)).thenReturn(new RiskRuleDto("5", "r2", "device", 3, "freeze", false, null));

        RiskRuleDto dto = riskRuleAppService.update(5L, req);

        assertThat(dto.name()).isEqualTo("r2");
        verify(riskRuleMapper).update(eq(req), eq(rule));
        verify(riskRuleRepository).save(rule);
    }

    @Test
    @DisplayName("删除规则调用仓库 delete")
    void delete_Existing_Deletes() {
        RiskRule rule = new RiskRule();
        when(riskRuleRepository.findById(5L)).thenReturn(Optional.of(rule));

        riskRuleAppService.delete(5L);

        verify(riskRuleRepository).delete(rule);
    }

    @Test
    @DisplayName("删除不存在规则时抛 NOT_FOUND")
    void delete_Missing_ThrowsNotFound() {
        when(riskRuleRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskRuleAppService.delete(9L)).isInstanceOf(BusinessException.class);
    }
}
