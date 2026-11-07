// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.data.spec.Specs;
import dev.strata.system.api.dto.LoginLogDto;
import dev.strata.system.api.dto.LoginLogQuery;
import dev.strata.system.ops.api.entity.LoginLog;
import dev.strata.system.ops.biz.mapper.LoginLogMapper;
import dev.strata.system.ops.biz.repository.LoginLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginLogAppService {

    private static final Logger log = LoggerFactory.getLogger(LoginLogAppService.class);

    private final LoginLogRepository loginLogRepository;
    private final LoginLogMapper loginLogMapper;

    public LoginLogAppService(LoginLogRepository loginLogRepository, LoginLogMapper loginLogMapper) {
        this.loginLogRepository = loginLogRepository;
        this.loginLogMapper = loginLogMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<LoginLogDto> page(LoginLogQuery query) {
        var spec = Specs.of(LoginLog.class)
                .like("username", query.username())
                .eq("status", query.status())
                .like("ip", query.ip())
                .ge("createdAt", query.from())
                .le("createdAt", query.to())
                .toPredicate();
        var pageable = PageRequests.of(query.page(), query.size(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LoginLog> page = loginLogRepository.findAll(spec, pageable);
        return PageResult.from(page.map(loginLogMapper::toDto));
    }
}
