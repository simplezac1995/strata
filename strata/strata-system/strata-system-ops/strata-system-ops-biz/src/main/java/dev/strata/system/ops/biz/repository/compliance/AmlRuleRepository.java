// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository.compliance;

import dev.strata.system.ops.api.entity.compliance.AmlRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AmlRuleRepository extends JpaRepository<AmlRule, Long>, JpaSpecificationExecutor<AmlRule> {

    List<AmlRule> findByRuleTypeAndEnabledTrue(String ruleType);
}
