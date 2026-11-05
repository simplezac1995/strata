// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.RegionDto;
import dev.strata.system.api.request.RegionCreateRequest;
import dev.strata.system.api.request.RegionUpdateRequest;
import dev.strata.system.config.api.entity.Region;
import dev.strata.system.config.biz.mapper.RegionMapper;
import dev.strata.system.config.biz.repository.RegionRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegionAppService {

    private static final Logger log = LoggerFactory.getLogger(RegionAppService.class);

    private final RegionRepository regionRepository;
    private final RegionMapper regionMapper;

    public RegionAppService(RegionRepository regionRepository, RegionMapper regionMapper) {
        this.regionRepository = regionRepository;
        this.regionMapper = regionMapper;
    }

    @Transactional(readOnly = true)
    public List<RegionDto> list() {
        return regionRepository.findAllByOrderByLevelAscIdAsc().stream()
                .map(regionMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<RegionDto> page(int page, int size) {
        return PageResult.from(regionRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "level", "id")))
                .map(regionMapper::toDto));
    }

    @Transactional(readOnly = true)
    public RegionDto get(Long id) {
        return regionMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "system:region", action = "create")
    public RegionDto create(RegionCreateRequest request) {
        if (regionRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Region code already exists");
        }
        Region region = regionMapper.toEntity(request);
        regionRepository.save(region);
        return regionMapper.toDto(region);
    }

    @Transactional
    @OperationLog(module = "system:region", action = "update")
    public RegionDto update(Long id, RegionUpdateRequest request) {
        Region region = require(id);
        regionMapper.update(request, region);
        regionRepository.save(region);
        return regionMapper.toDto(region);
    }

    @Transactional
    @OperationLog(module = "system:region", action = "delete")
    public void delete(Long id) {
        Region region = require(id);
        if (regionRepository.countByParentId(id) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "Region has children");
        }
        regionRepository.delete(region);
    }

    private Region require(Long id) {
        return regionRepository.findById(id).orElseThrow(() -> new NotFoundException("Region not found"));
    }
}
