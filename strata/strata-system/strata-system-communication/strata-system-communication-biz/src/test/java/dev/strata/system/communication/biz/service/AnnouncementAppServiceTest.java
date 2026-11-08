// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.AnnouncementDto;
import dev.strata.system.api.request.AnnouncementCreateRequest;
import dev.strata.system.api.request.AnnouncementUpdateRequest;
import dev.strata.system.communication.api.entity.Announcement;
import dev.strata.system.communication.biz.mapper.AnnouncementMapper;
import dev.strata.system.communication.biz.repository.AnnouncementRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AnnouncementAppServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementMapper announcementMapper;

    @InjectMocks
    private AnnouncementAppService announcementAppService;

    private AnnouncementDto dto() {
        return new AnnouncementDto("1", "title", "body", true, Instant.now(), Instant.now());
    }

    @Nested
    class QueryTests {

        @Test
        void list_MapsAll() {
            when(announcementRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(new Announcement()));
            when(announcementMapper.toDto(any(Announcement.class))).thenReturn(dto());

            List<AnnouncementDto> result = announcementAppService.list();

            assertThat(result).hasSize(1);
        }

        @Test
        void get_Found() {
            when(announcementRepository.findById(1L)).thenReturn(Optional.of(new Announcement()));
            when(announcementMapper.toDto(any(Announcement.class))).thenReturn(dto());

            AnnouncementDto result = announcementAppService.get(1L);

            assertThat(result.title()).isEqualTo("title");
        }

        @Test
        void get_NotFound_Throws() {
            when(announcementRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> announcementAppService.get(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        void page_ReturnsPaged() {
            when(announcementRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(new Announcement())));
            when(announcementMapper.toDto(any(Announcement.class))).thenReturn(dto());

            PageResult<AnnouncementDto> result = announcementAppService.page(0, 10);

            assertThat(result.content()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("create / update / delete")
    class MutationTests {

        @Test
        void create_Success() {
            AnnouncementCreateRequest request = new AnnouncementCreateRequest("t", "b", true);
            Announcement entity = new Announcement();
            when(announcementMapper.toEntity(request)).thenReturn(entity);
            when(announcementRepository.save(entity)).thenReturn(entity);
            when(announcementMapper.toDto(entity)).thenReturn(dto());

            AnnouncementDto result = announcementAppService.create(request);

            assertThat(result.title()).isEqualTo("title");
            verify(announcementRepository).save(entity);
        }

        @Test
        void update_Success() {
            Announcement existing = new Announcement();
            AnnouncementUpdateRequest request = new AnnouncementUpdateRequest("t2", "b2", false);
            when(announcementRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(announcementRepository.save(existing)).thenReturn(existing);
            when(announcementMapper.toDto(existing)).thenReturn(dto());

            announcementAppService.update(1L, request);

            verify(announcementMapper).update(eq(request), any(Announcement.class));
            verify(announcementRepository).save(existing);
        }

        @Test
        void update_NotFound_Throws() {
            AnnouncementUpdateRequest request = new AnnouncementUpdateRequest("t", "b", true);
            when(announcementRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> announcementAppService.update(99L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(announcementRepository, never()).save(any(Announcement.class));
        }

        @Test
        void delete_Success() {
            when(announcementRepository.findById(1L)).thenReturn(Optional.of(new Announcement()));

            announcementAppService.delete(1L);

            verify(announcementRepository).delete(any(Announcement.class));
        }

        @Test
        void delete_NotFound_Throws() {
            when(announcementRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> announcementAppService.delete(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(announcementRepository, never()).delete(any(Announcement.class));
        }
    }
}
