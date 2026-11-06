// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.system.api.dto.SiteSettingDto;
import dev.strata.system.config.api.entity.SiteSetting;
import dev.strata.system.config.biz.mapper.SiteSettingMapper;
import dev.strata.system.config.biz.repository.SiteSettingRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteSettingAppService {

    private static final Logger log = LoggerFactory.getLogger(SiteSettingAppService.class);

    private final SiteSettingRepository siteSettingRepository;
    private final SiteSettingMapper siteSettingMapper;

    public SiteSettingAppService(SiteSettingRepository siteSettingRepository, SiteSettingMapper siteSettingMapper) {
        this.siteSettingRepository = siteSettingRepository;
        this.siteSettingMapper = siteSettingMapper;
    }

    @Transactional(readOnly = true)
    public List<SiteSettingDto> getAll() {
        return siteSettingRepository.findAllByOrderBySettingKeyAsc().stream()
                .map(siteSettingMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, String> getAllAsMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (SiteSetting setting : siteSettingRepository.findAllByOrderBySettingKeyAsc()) {
            map.put(setting.getSettingKey(), setting.getSettingValue());
        }
        return map;
    }

    @Transactional
    @OperationLog(module = "system:sitesetting", action = "update")
    public Map<String, String> putAll(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return getAllAsMap();
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            SiteSetting setting = siteSettingRepository.findById(entry.getKey()).orElseGet(SiteSetting::new);
            setting.setSettingKey(entry.getKey());
            setting.setSettingValue(entry.getValue());
            siteSettingRepository.save(setting);
        }
        return getAllAsMap();
    }
}
