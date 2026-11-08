// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.system.api.dto.OpsAnnouncementDto;
import dev.strata.system.api.request.OpsAnnouncementUpsertRequest;
import dev.strata.system.ops.api.entity.Announcement;
import dev.strata.system.ops.biz.mapper.OpsAnnouncementMapper;
import dev.strata.system.ops.biz.repository.OpsAnnouncementRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpsAnnouncementAppService {

    private final OpsAnnouncementRepository announcementRepository;
    private final OpsAnnouncementMapper announcementMapper;

    public OpsAnnouncementAppService(
            OpsAnnouncementRepository announcementRepository, OpsAnnouncementMapper announcementMapper) {
        this.announcementRepository = announcementRepository;
        this.announcementMapper = announcementMapper;
    }

    @Transactional(readOnly = true)
    public List<OpsAnnouncementDto> list() {
        return announcementRepository.findAllByOrderBySortAscIdAsc().stream()
                .map(announcementMapper::toDto)
                .toList();
    }

    /** Currently published announcements inside their schedule window (player-facing). */
    @Transactional(readOnly = true)
    public List<OpsAnnouncementDto> active() {
        return announcementRepository.findActive(Instant.now()).stream()
                .map(announcementMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OpsAnnouncementDto get(Long id) {
        return announcementMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "ops:announcement", action = "create")
    public OpsAnnouncementDto create(OpsAnnouncementUpsertRequest request) {
        Announcement announcement = announcementMapper.toEntity(request);
        announcementRepository.save(announcement);
        return announcementMapper.toDto(announcement);
    }

    @Transactional
    @OperationLog(module = "ops:announcement", action = "update")
    public OpsAnnouncementDto update(Long id, OpsAnnouncementUpsertRequest request) {
        Announcement announcement = require(id);
        announcementMapper.update(request, announcement);
        announcementRepository.save(announcement);
        return announcementMapper.toDto(announcement);
    }

    @Transactional
    @OperationLog(module = "ops:announcement", action = "delete")
    public void delete(Long id) {
        announcementRepository.delete(require(id));
    }

    private Announcement require(Long id) {
        return announcementRepository.findById(id).orElseThrow(() -> new NotFoundException("Announcement not found"));
    }
}
