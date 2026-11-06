// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.listener;

import dev.strata.system.api.event.LoginLogEvent;
import dev.strata.system.ops.biz.mapper.LoginLogMapper;
import dev.strata.system.ops.biz.repository.LoginLogRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class LoginLogEventListener {

    private final LoginLogRepository loginLogRepository;
    private final LoginLogMapper loginLogMapper;

    public LoginLogEventListener(LoginLogRepository loginLogRepository, LoginLogMapper loginLogMapper) {
        this.loginLogRepository = loginLogRepository;
        this.loginLogMapper = loginLogMapper;
    }

    /**
     * Plain async listener (not transactional): failed logins roll back the caller's
     * transaction, so the log must be written outside it. The repository save runs in
     * its own transaction on the executor thread.
     */
    @Async("operationLogExecutor")
    @EventListener
    public void onLoginLog(LoginLogEvent event) {
        loginLogRepository.save(loginLogMapper.toEntity(event));
    }
}
