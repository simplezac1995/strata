// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.system.api.dto.compliance.GeoCheckResult;
import dev.strata.system.api.request.compliance.BlockedRegionRequest;
import dev.strata.system.api.request.compliance.GeoRestrictionCreateRequest;
import dev.strata.system.ops.api.entity.compliance.GeoRestriction;
import dev.strata.system.ops.biz.mapper.compliance.GeoRestrictionMapper;
import dev.strata.system.ops.biz.repository.compliance.GeoRestrictionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeoAppService 单元测试")
class GeoAppServiceTest {

    @Mock
    private GeoRestrictionRepository geoRestrictionRepository;

    @Mock
    private GeoRestrictionMapper geoRestrictionMapper;

    @Mock
    private dev.strata.system.ops.biz.repository.compliance.BlockedRegionRepository blockedRegionRepository;

    @Mock
    private dev.strata.system.ops.biz.mapper.compliance.BlockedRegionMapper blockedRegionMapper;

    @InjectMocks
    private GeoAppService service;

    @Test
    @DisplayName("创建封禁国家时大写化并落库")
    void createRegion_UppercasesCode() {
        BlockedRegionRequest req = new BlockedRegionRequest("us", "United States", true, true);
        when(blockedRegionRepository.existsByCountryCode("US")).thenReturn(false);
        dev.strata.system.ops.api.entity.compliance.BlockedRegion row =
                new dev.strata.system.ops.api.entity.compliance.BlockedRegion();
        when(blockedRegionMapper.toEntity(req)).thenReturn(row);
        service.createRegion(req);
        assertThat(row.getCountryCode()).isEqualTo("US");
        verify(blockedRegionRepository).save(row);
    }

    @Test
    @DisplayName("重复国家抛 CONFLICT")
    void createRegion_Duplicate_Throws() {
        BlockedRegionRequest req = new BlockedRegionRequest("US", "United States", true, true);
        when(blockedRegionRepository.existsByCountryCode("US")).thenReturn(true);
        assertThatThrownBy(() -> service.createRegion(req)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("命中 IP 范围返回 blocked")
    void checkIp_RangeHit_Blocked() {
        // The framework uses lexicographic range containment (real CIDR comparison is TODO), so this range is
        // lexicographically self-consistent: 10.0.0.0 <= 10.0.0.5 <= 10.0.0.9.
        GeoRestriction r = new GeoRestriction();
        r.setIpRangeStart("10.0.0.0");
        r.setIpRangeEnd("10.0.0.9");
        r.setAction("block");
        r.setReason("VPN");
        when(geoRestrictionRepository.findByEnabledTrue()).thenReturn(List.of(r));

        GeoCheckResult result = service.checkIp("10.0.0.5");

        assertThat(result.blocked()).isTrue();
        assertThat(result.reason()).isEqualTo("VPN");
    }

    @Test
    @DisplayName("未命中返回未封禁")
    void checkIp_NoRange_NotBlocked() {
        when(geoRestrictionRepository.findByEnabledTrue()).thenReturn(List.of());
        GeoCheckResult result = service.checkIp("8.8.8.8");
        assertThat(result.blocked()).isFalse();
    }

    @Test
    @DisplayName("更新限制存在时调用 update")
    void updateRestriction_Existing_Updates() {
        GeoRestriction row = new GeoRestriction();
        when(geoRestrictionRepository.findById(5L)).thenReturn(Optional.of(row));
        when(geoRestrictionRepository.save(row)).thenReturn(row);
        var req = new dev.strata.system.api.request.compliance.GeoRestrictionUpdateRequest(
                "1.0.0.1", "1.0.0.9", "US", "block", "r", true);
        service.updateRestriction(5L, req);
        verify(geoRestrictionMapper).update(req, row);
    }

    @Test
    @DisplayName("删除不存在抛 NOT_FOUND")
    void deleteRestriction_Missing_Throws() {
        when(geoRestrictionRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteRestriction(9L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("创建限制落库")
    void createRestriction_Persists() {
        GeoRestrictionCreateRequest req =
                new GeoRestrictionCreateRequest("1.0.0.1", "1.0.0.9", "US", "block", "r", true);
        GeoRestriction row = new GeoRestriction();
        when(geoRestrictionMapper.toEntity(req)).thenReturn(row);
        when(geoRestrictionRepository.save(row)).thenReturn(row);
        service.createRestriction(req);
        verify(geoRestrictionRepository).save(row);
    }
}
