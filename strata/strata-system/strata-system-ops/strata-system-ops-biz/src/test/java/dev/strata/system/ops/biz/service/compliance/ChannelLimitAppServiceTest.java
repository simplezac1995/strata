// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.system.api.request.compliance.ChannelLimitCreateRequest;
import dev.strata.system.ops.api.entity.compliance.ChannelLimit;
import dev.strata.system.ops.biz.mapper.compliance.ChannelLimitMapper;
import dev.strata.system.ops.biz.repository.compliance.ChannelLimitRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelLimitAppService 单元测试")
class ChannelLimitAppServiceTest {

    @Mock
    private ChannelLimitRepository channelLimitRepository;

    @Mock
    private ChannelLimitMapper channelLimitMapper;

    @InjectMocks
    private ChannelLimitAppService service;

    @Test
    @DisplayName("玩家渠道只返回启用项")
    void myChannels_OnlyEnabled() {
        when(channelLimitRepository.findByEnabledTrue()).thenReturn(List.of(new ChannelLimit()));
        when(channelLimitMapper.toDto(any(ChannelLimit.class)))
                .thenReturn(new dev.strata.system.api.dto.compliance.ChannelLimitDto(
                        "1", "tron", "deposit", 1L, 2L, 3L, null, true, null));
        assertThat(service.myChannels()).hasSize(1);
    }

    @Test
    @DisplayName("新建唯一渠道落库")
    void create_New_Persists() {
        ChannelLimitCreateRequest req = new ChannelLimitCreateRequest("tron", "deposit", 1L, 100L, 1000L, null, true);
        when(channelLimitRepository.existsByChannelAndDirection("tron", "deposit"))
                .thenReturn(false);
        ChannelLimit row = new ChannelLimit();
        when(channelLimitMapper.toEntity(req)).thenReturn(row);
        when(channelLimitRepository.save(row)).thenReturn(row);
        service.create(req);
        verify(channelLimitRepository).save(row);
    }

    @Test
    @DisplayName("重复渠道抛 CONFLICT")
    void create_Duplicate_Throws() {
        ChannelLimitCreateRequest req = new ChannelLimitCreateRequest("tron", "deposit", 1L, 100L, 1000L, null, true);
        when(channelLimitRepository.existsByChannelAndDirection("tron", "deposit"))
                .thenReturn(true);
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("更新存在时更新")
    void update_Existing_Updates() {
        ChannelLimit row = new ChannelLimit();
        when(channelLimitRepository.findById(5L)).thenReturn(Optional.of(row));
        when(channelLimitRepository.save(row)).thenReturn(row);
        var req = new dev.strata.system.api.request.compliance.ChannelLimitUpdateRequest(
                "tron", "withdraw", 1L, 100L, 1000L, null, true);
        service.update(5L, req);
        verify(channelLimitMapper).update(req, row);
    }

    @Test
    @DisplayName("删除不存在抛 NOT_FOUND")
    void delete_Missing_Throws() {
        when(channelLimitRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(BusinessException.class);
    }
}
