// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository;

import dev.strata.system.ops.api.entity.PopupConfig;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopupConfigRepository extends JpaRepository<PopupConfig, Long> {

    List<PopupConfig> findAllByOrderByIdAsc();
}
