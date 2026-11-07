// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository;

import dev.strata.system.ops.api.entity.SignInConfig;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignInConfigRepository extends JpaRepository<SignInConfig, Long> {

    List<SignInConfig> findAllByOrderByDayAsc();

    SignInConfig findByDay(int day);
}
