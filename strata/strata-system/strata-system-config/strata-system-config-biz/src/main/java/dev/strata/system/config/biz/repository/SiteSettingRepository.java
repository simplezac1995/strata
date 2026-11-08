// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.repository;

import dev.strata.system.config.api.entity.SiteSetting;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSettingRepository extends JpaRepository<SiteSetting, String> {

    List<SiteSetting> findAllByOrderBySettingKeyAsc();
}
