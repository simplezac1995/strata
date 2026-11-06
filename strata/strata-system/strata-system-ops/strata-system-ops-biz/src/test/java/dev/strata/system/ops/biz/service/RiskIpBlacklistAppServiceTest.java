// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.RiskIpBlacklistDto;
import dev.strata.system.api.request.RiskIpBlacklistCreateRequest;
import dev.strata.system.ops.api.entity.RiskIpBlacklist;
import dev.strata.system.ops.biz.mapper.RiskIpBlacklistMapper;
import dev.strata.system.ops.biz.repository.RiskIpBlacklistRepository;
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
@DisplayName("RiskIpBlacklistAppService 单元测试")
class RiskIpBlacklistAppServiceTest {

    @Mock
    private RiskIpBlacklistRepository riskIpBlacklistRepository;

    @Mock
    private RiskIpBlacklistMapper riskIpBlacklistMapper;

    @InjectMocks
    private RiskIpBlacklistAppService service;

    @Test
    @DisplayName("分页查询黑名单")
    void page_ReturnsPaged() {
        RiskIpBlacklist row = new RiskIpBlacklist();
        when(riskIpBlacklistRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(row)));
        when(riskIpBlacklistMapper.toDto(any(RiskIpBlacklist.class)))
                .thenReturn(new RiskIpBlacklistDto("1", "1.2.3.4", "bad", null, null));

        PageResult<RiskIpBlacklistDto> result = service.page(0, 10);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("新增未重复 IP 时保存")
    void create_NewIp_Persists() {
        RiskIpBlacklistCreateRequest req = new RiskIpBlacklistCreateRequest(" 1.2.3.4 ", "abuse", null);
        when(riskIpBlacklistRepository.existsByIpAddress("1.2.3.4")).thenReturn(false);
        RiskIpBlacklist row = new RiskIpBlacklist();
        when(riskIpBlacklistMapper.toEntity(req)).thenReturn(row);
        when(riskIpBlacklistRepository.save(row)).thenReturn(row);
        when(riskIpBlacklistMapper.toDto(row)).thenReturn(new RiskIpBlacklistDto("1", "1.2.3.4", "abuse", null, null));

        RiskIpBlacklistDto dto = service.create(req);

        assertThat(dto.ipAddress()).isEqualTo("1.2.3.4");
        verify(riskIpBlacklistRepository).save(row);
    }

    @Test
    @DisplayName("新增已存在 IP 时抛 CONFLICT")
    void create_DuplicateIp_ThrowsConflict() {
        RiskIpBlacklistCreateRequest req = new RiskIpBlacklistCreateRequest("1.2.3.4", "abuse", null);
        when(riskIpBlacklistRepository.existsByIpAddress("1.2.3.4")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("删除存在记录时调用仓库 delete")
    void delete_Existing_Deletes() {
        RiskIpBlacklist row = new RiskIpBlacklist();
        when(riskIpBlacklistRepository.findById(5L)).thenReturn(Optional.of(row));

        service.delete(5L);

        verify(riskIpBlacklistRepository).delete(row);
    }

    @Test
    @DisplayName("删除不存在记录时抛 NOT_FOUND")
    void delete_Missing_ThrowsNotFound() {
        when(riskIpBlacklistRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(BusinessException.class);
    }
}
