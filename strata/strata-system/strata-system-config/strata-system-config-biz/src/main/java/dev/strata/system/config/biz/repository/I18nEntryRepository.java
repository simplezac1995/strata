// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.repository;

import dev.strata.system.config.api.entity.I18nEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface I18nEntryRepository extends JpaRepository<I18nEntry, Long>, JpaSpecificationExecutor<I18nEntry> {

    boolean existsByMessageKeyAndDeletedFalse(String messageKey);
}
