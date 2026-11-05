// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.system.api.dto.OpsAnnouncementDto;
import dev.strata.system.api.request.OpsAnnouncementUpsertRequest;
import dev.strata.system.ops.api.entity.Announcement;
import dev.strata.system.ops.biz.mapper.OpsAnnouncementMapper;
import dev.strata.system.ops.biz.repository.OpsAnnouncementRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnnouncementAppService 单元测试")
class OpsAnnouncementAppServiceTest {

    @Mock
    private OpsAnnouncementRepository announcementRepository;

    @Mock
    private OpsAnnouncementMapper announcementMapper;

    private OpsAnnouncementAppService service;

    @BeforeEach
    void setUp() {
        service = new OpsAnnouncementAppService(announcementRepository, announcementMapper);
    }

    private Announcement announcement(long id) {
        Announcement a = new Announcement();
        a.setTitle("t");
        a.setType("notice");
        a.setStatus("published");
        return a;
    }

    private OpsAnnouncementDto dto() {
        return new OpsAnnouncementDto("1", "t", "c", "notice", null, null, "published", 0, null);
    }

    @Nested
    @DisplayName("查询")
    class QueryTests {

        @Test
        @DisplayName("list 返回全部")
        void list_MapsAll() {
            when(announcementRepository.findAllByOrderBySortAscIdAsc()).thenReturn(List.of(announcement(1L)));
            when(announcementMapper.toDto(any(Announcement.class))).thenReturn(dto());

            List<OpsAnnouncementDto> result = service.list();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("active 走仓库活跃窗口")
        void active_DelegatesToRepository() {
            when(announcementRepository.findActive(any())).thenReturn(List.of(announcement(1L)));
            when(announcementMapper.toDto(any(Announcement.class))).thenReturn(dto());

            assertThat(service.active()).hasSize(1);
        }

        @Test
        @DisplayName("get 存在时返回 DTO")
        void get_Found() {
            when(announcementRepository.findById(1L)).thenReturn(Optional.of(announcement(1L)));
            when(announcementMapper.toDto(any(Announcement.class))).thenReturn(dto());

            assertThat(service.get(1L).title()).isEqualTo("t");
        }

        @Test
        @DisplayName("get 不存在抛 NOT_FOUND")
        void get_NotFound_Throws() {
            when(announcementRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("写操作")
    class MutationTests {

        @Test
        @DisplayName("create 保存并返回")
        void create_Success() {
            OpsAnnouncementUpsertRequest request =
                    new OpsAnnouncementUpsertRequest("t", "c", "notice", null, null, "published", 0);
            when(announcementMapper.toEntity(request)).thenReturn(announcement(0L));
            when(announcementRepository.save(any(Announcement.class))).thenReturn(announcement(1L));
            when(announcementMapper.toDto(any(Announcement.class))).thenReturn(dto());

            service.create(request);

            verify(announcementRepository).save(any(Announcement.class));
        }

        @Test
        @DisplayName("update 更新已存在行")
        void update_Success() {
            OpsAnnouncementUpsertRequest request =
                    new OpsAnnouncementUpsertRequest("t", "c", "banner", null, null, "draft", 1);
            when(announcementRepository.findById(1L)).thenReturn(Optional.of(announcement(1L)));
            when(announcementRepository.save(any(Announcement.class))).thenReturn(announcement(1L));
            when(announcementMapper.toDto(any(Announcement.class))).thenReturn(dto());

            service.update(1L, request);

            verify(announcementMapper).update(eq(request), any(Announcement.class));
            verify(announcementRepository).save(any(Announcement.class));
        }

        @Test
        @DisplayName("delete 删除")
        void delete_Success() {
            when(announcementRepository.findById(1L)).thenReturn(Optional.of(announcement(1L)));

            service.delete(1L);

            verify(announcementRepository).delete(any(Announcement.class));
        }

        @Test
        @DisplayName("delete 不存在抛 NOT_FOUND 且不删除")
        void delete_NotFound_Throws() {
            when(announcementRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(BusinessException.class);

            verify(announcementRepository, never()).delete(any(Announcement.class));
        }
    }
}
