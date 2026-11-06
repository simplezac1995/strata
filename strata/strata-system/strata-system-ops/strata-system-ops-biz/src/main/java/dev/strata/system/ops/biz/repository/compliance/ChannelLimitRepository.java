// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository.compliance;

import dev.strata.system.ops.api.entity.compliance.ChannelLimit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ChannelLimitRepository
        extends JpaRepository<ChannelLimit, Long>, JpaSpecificationExecutor<ChannelLimit> {

    boolean existsByChannelAndDirection(String channel, String direction);

    List<ChannelLimit> findByEnabledTrue();
}
