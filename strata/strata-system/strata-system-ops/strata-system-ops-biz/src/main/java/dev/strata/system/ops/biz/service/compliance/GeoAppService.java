// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service.compliance;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.data.spec.Specs;
import dev.strata.system.api.dto.compliance.BlockedRegionDto;
import dev.strata.system.api.dto.compliance.GeoCheckResult;
import dev.strata.system.api.dto.compliance.GeoRestrictionDto;
import dev.strata.system.api.request.compliance.BlockedRegionRequest;
import dev.strata.system.api.request.compliance.GeoRestrictionCreateRequest;
import dev.strata.system.api.request.compliance.GeoRestrictionUpdateRequest;
import dev.strata.system.ops.api.entity.compliance.BlockedRegion;
import dev.strata.system.ops.api.entity.compliance.GeoRestriction;
import dev.strata.system.ops.biz.mapper.compliance.BlockedRegionMapper;
import dev.strata.system.ops.biz.mapper.compliance.GeoRestrictionMapper;
import dev.strata.system.ops.biz.repository.compliance.BlockedRegionRepository;
import dev.strata.system.ops.biz.repository.compliance.GeoRestrictionRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Geo-fencing: IP range restrictions and country-level blocked regions. */
@Service
public class GeoAppService {

    private static final Logger log = LoggerFactory.getLogger(GeoAppService.class);

    private final GeoRestrictionRepository geoRestrictionRepository;
    private final GeoRestrictionMapper geoRestrictionMapper;
    private final BlockedRegionRepository blockedRegionRepository;
    private final BlockedRegionMapper blockedRegionMapper;

    public GeoAppService(
            GeoRestrictionRepository geoRestrictionRepository,
            GeoRestrictionMapper geoRestrictionMapper,
            BlockedRegionRepository blockedRegionRepository,
            BlockedRegionMapper blockedRegionMapper) {
        this.geoRestrictionRepository = geoRestrictionRepository;
        this.geoRestrictionMapper = geoRestrictionMapper;
        this.blockedRegionRepository = blockedRegionRepository;
        this.blockedRegionMapper = blockedRegionMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<GeoRestrictionDto> pageRestrictions(String countryCode, Boolean enabled, int page, int size) {
        var spec = Specs.of(GeoRestriction.class)
                .eq("countryCode", countryCode)
                .eq("enabled", enabled)
                .toSpecification();
        Page<GeoRestriction> result =
                geoRestrictionRepository.findAll(spec, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.from(result.map(geoRestrictionMapper::toDto));
    }

    @Transactional
    @OperationLog(module = "compliance:geo", action = "create")
    public GeoRestrictionDto createRestriction(GeoRestrictionCreateRequest request) {
        GeoRestriction row = geoRestrictionMapper.toEntity(request);
        geoRestrictionRepository.save(row);
        return geoRestrictionMapper.toDto(row);
    }

    @Transactional
    @OperationLog(module = "compliance:geo", action = "update")
    public GeoRestrictionDto updateRestriction(Long id, GeoRestrictionUpdateRequest request) {
        GeoRestriction row = requireRestriction(id);
        geoRestrictionMapper.update(request, row);
        geoRestrictionRepository.save(row);
        return geoRestrictionMapper.toDto(row);
    }

    @Transactional
    @OperationLog(module = "compliance:geo", action = "delete")
    public void deleteRestriction(Long id) {
        geoRestrictionRepository.delete(requireRestriction(id));
    }

    @Transactional(readOnly = true)
    public List<BlockedRegionDto> listRegions() {
        return blockedRegionRepository.findAll(Sort.by(Sort.Direction.ASC, "countryCode")).stream()
                .map(blockedRegionMapper::toDto)
                .toList();
    }

    @Transactional
    @OperationLog(module = "compliance:geo", action = "create")
    public BlockedRegionDto createRegion(BlockedRegionRequest request) {
        String code = request.countryCode().toUpperCase();
        if (blockedRegionRepository.existsByCountryCode(code)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Country already blocked");
        }
        BlockedRegion row = blockedRegionMapper.toEntity(request);
        row.setCountryCode(code);
        blockedRegionRepository.save(row);
        return blockedRegionMapper.toDto(row);
    }

    @Transactional
    @OperationLog(module = "compliance:geo", action = "delete")
    public void deleteRegion(Long id) {
        blockedRegionRepository.delete(blockedRegionRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Blocked region not found")));
    }

    /**
     * Best-effort IP check. Real CIDR / inet-pton comparison is TODO; this framework uses plain lexicographic
     * range containment so the admin contract is wired end-to-end.
     */
    @Transactional(readOnly = true)
    public GeoCheckResult checkIp(String ip) {
        String target = ip.trim();
        for (GeoRestriction r : geoRestrictionRepository.findByEnabledTrue()) {
            if (target.compareTo(r.getIpRangeStart()) >= 0 && target.compareTo(r.getIpRangeEnd()) <= 0) {
                return new GeoCheckResult("block".equals(r.getAction()), r.getAction(), r.getReason());
            }
        }
        return new GeoCheckResult(false, null, null);
    }

    private GeoRestriction requireRestriction(Long id) {
        return geoRestrictionRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Geo restriction not found"));
    }
}
