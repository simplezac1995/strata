// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.user;

import java.util.Optional;

public interface UserLookup {

    Optional<UserReference> findByUsername(String username);

    record UserReference(Long id, String username) {}
}
