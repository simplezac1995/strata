// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.system.api.dto.PopupConfigDto;
import dev.strata.system.api.request.PopupUpsertRequest;
import dev.strata.system.ops.api.entity.PopupConfig;
import dev.strata.system.ops.biz.mapper.PopupConfigMapper;
import dev.strata.system.ops.biz.repository.PopupConfigRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PopupConfigAppServiceTest {

    @Mock
    private PopupConfigRepository popupConfigRepository;

    @Mock
    private PopupConfigMapper popupConfigMapper;

    private PopupConfigAppService service;

    @BeforeEach
    void setUp() {
        service = new PopupConfigAppService(popupConfigRepository, popupConfigMapper);
    }

    private PopupConfig popup() {
        PopupConfig p = new PopupConfig();
        p.setName("welcome");
        return p;
    }

    private PopupConfigDto dto() {
        return new PopupConfigDto("1", "welcome", null, null, null, null, "always", true, null);
    }

    @Test
    void list_MapsAll() {
        when(popupConfigRepository.findAllByOrderByIdAsc()).thenReturn(List.of(popup()));
        when(popupConfigMapper.toDto(any(PopupConfig.class))).thenReturn(dto());

        assertThat(service.list()).hasSize(1);
    }

    @Test
    void get_Found() {
        when(popupConfigRepository.findById(1L)).thenReturn(Optional.of(popup()));
        when(popupConfigMapper.toDto(any(PopupConfig.class))).thenReturn(dto());

        assertThat(service.get(1L).name()).isEqualTo("welcome");
    }

    @Test
    void get_NotFound_Throws() {
        when(popupConfigRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void create_Success() {
        PopupUpsertRequest request = new PopupUpsertRequest("welcome", "img", "link", null, null, "always", true);
        when(popupConfigMapper.toEntity(request)).thenReturn(popup());
        when(popupConfigRepository.save(any(PopupConfig.class))).thenReturn(popup());
        when(popupConfigMapper.toDto(any(PopupConfig.class))).thenReturn(dto());

        service.create(request);

        verify(popupConfigRepository).save(any(PopupConfig.class));
    }

    @Test
    void update_Success() {
        PopupUpsertRequest request = new PopupUpsertRequest("welcome", "img", "link", null, null, "once", false);
        when(popupConfigRepository.findById(1L)).thenReturn(Optional.of(popup()));
        when(popupConfigRepository.save(any(PopupConfig.class))).thenReturn(popup());
        when(popupConfigMapper.toDto(any(PopupConfig.class))).thenReturn(dto());

        service.update(1L, request);

        verify(popupConfigMapper).update(eq(request), any(PopupConfig.class));
    }

    @Test
    void delete_Success() {
        when(popupConfigRepository.findById(1L)).thenReturn(Optional.of(popup()));

        service.delete(1L);

        verify(popupConfigRepository).delete(any(PopupConfig.class));
    }
}
