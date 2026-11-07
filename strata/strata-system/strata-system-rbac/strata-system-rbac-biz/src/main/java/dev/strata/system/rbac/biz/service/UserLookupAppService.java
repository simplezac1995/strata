// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import dev.strata.system.api.user.UserLookup;
import dev.strata.system.rbac.biz.repository.UserRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserLookupAppService implements UserLookup {

    private static final Logger log = LoggerFactory.getLogger(UserLookupAppService.class);

    private final UserRepository userRepository;

    public UserLookupAppService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserReference> findByUsername(String username) {
        return userRepository
                .findByUsernameAndDeletedFalse(username)
                .map(user -> new UserReference(user.getId(), user.getUsername()));
    }
}
