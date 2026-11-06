// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.service;

import dev.strata.ai.api.request.AiSkillCreateRequest;
import dev.strata.ai.api.request.AiSkillUpdateRequest;
import dev.strata.ai.config.api.dto.AiSkillDto;
import dev.strata.ai.config.api.entity.AiSkill;
import dev.strata.ai.config.biz.mapper.AiSkillMapper;
import dev.strata.ai.config.biz.repository.AiSkillRepository;
import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiSkillAppService {

    private static final Logger log = LoggerFactory.getLogger(AiSkillAppService.class);

    private final AiSkillRepository aiSkillRepository;
    private final AiSkillMapper aiSkillMapper;

    public AiSkillAppService(AiSkillRepository aiSkillRepository, AiSkillMapper aiSkillMapper) {
        this.aiSkillRepository = aiSkillRepository;
        this.aiSkillMapper = aiSkillMapper;
    }

    @Transactional(readOnly = true)
    public List<AiSkillDto> list() {
        return aiSkillRepository.findAllByOrderByIdAsc().stream()
                .map(aiSkillMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<AiSkillDto> page(String type, int page, int size) {
        var pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        var result = type == null || type.isBlank()
                ? aiSkillRepository.findAll(pageable)
                : aiSkillRepository.findByType(type, pageable);
        return PageResult.from(result.map(aiSkillMapper::toDto));
    }

    @Transactional(readOnly = true)
    public AiSkillDto get(Long id) {
        return aiSkillMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "ai:skill", action = "create")
    public AiSkillDto create(AiSkillCreateRequest request) {
        AiSkill skill = aiSkillMapper.toEntity(request);
        aiSkillRepository.save(skill);
        return aiSkillMapper.toDto(skill);
    }

    @Transactional
    @OperationLog(module = "ai:skill", action = "update")
    public AiSkillDto update(Long id, AiSkillUpdateRequest request) {
        AiSkill skill = require(id);
        aiSkillMapper.update(request, skill);
        aiSkillRepository.save(skill);
        return aiSkillMapper.toDto(skill);
    }

    @Transactional
    @OperationLog(module = "ai:skill", action = "delete")
    public void delete(Long id) {
        aiSkillRepository.delete(require(id));
    }

    private AiSkill require(Long id) {
        return aiSkillRepository.findById(id).orElseThrow(() -> new NotFoundException("Skill not found"));
    }
}
