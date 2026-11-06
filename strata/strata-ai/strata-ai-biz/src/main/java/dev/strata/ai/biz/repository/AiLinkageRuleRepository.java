// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.repository;

import dev.strata.ai.biz.entity.AiLinkageRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AiLinkageRuleRepository
        extends JpaRepository<AiLinkageRule, Long>, JpaSpecificationExecutor<AiLinkageRule> {

    List<AiLinkageRule> findByEnabledTrueAndObjectAndEvent(String object, String event);
}
