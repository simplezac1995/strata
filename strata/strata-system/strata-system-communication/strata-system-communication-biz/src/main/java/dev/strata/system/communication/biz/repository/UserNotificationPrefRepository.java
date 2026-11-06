// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.repository;

import dev.strata.system.communication.api.entity.UserNotificationPref;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationPrefRepository extends JpaRepository<UserNotificationPref, Long> {}
