// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.AnnouncementDto;
import dev.strata.system.api.request.AnnouncementCreateRequest;
import dev.strata.system.api.request.AnnouncementUpdateRequest;
import dev.strata.system.communication.api.entity.Announcement;
import dev.strata.system.communication.biz.mapper.AnnouncementMapper;
import dev.strata.system.communication.biz.repository.AnnouncementRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementAppService {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementAppService.class);

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementMapper announcementMapper;

    public AnnouncementAppService(
            AnnouncementRepository announcementRepository, AnnouncementMapper announcementMapper) {
        this.announcementRepository = announcementRepository;
        this.announcementMapper = announcementMapper;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> list() {
        return announcementRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(announcementMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnnouncementDto get(Long id) {
        return announcementMapper.toDto(require(id));
    }

    @Transactional(readOnly = true)
    public PageResult<AnnouncementDto> page(Integer page, Integer size) {
        Pageable pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return PageResult.from(announcementRepository.findAll(pageable).map(announcementMapper::toDto));
    }

    @Transactional
    @OperationLog(module = "system:announcement", action = "create")
    public AnnouncementDto create(AnnouncementCreateRequest request) {
        Announcement announcement = announcementMapper.toEntity(request);
        announcementRepository.save(announcement);
        return announcementMapper.toDto(announcement);
    }

    @Transactional
    @OperationLog(module = "system:announcement", action = "update")
    public AnnouncementDto update(Long id, AnnouncementUpdateRequest request) {
        Announcement announcement = require(id);
        announcementMapper.update(request, announcement);
        announcementRepository.save(announcement);
        return announcementMapper.toDto(announcement);
    }

    @Transactional
    @OperationLog(module = "system:announcement", action = "delete")
    public void delete(Long id) {
        announcementRepository.delete(require(id));
    }

    private Announcement require(Long id) {
        return announcementRepository.findById(id).orElseThrow(() -> new NotFoundException("Announcement not found"));
    }
}
