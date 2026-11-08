// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository;

import dev.strata.system.ops.api.entity.SignInRecord;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignInRecordRepository extends JpaRepository<SignInRecord, Long> {

    boolean existsByUserIdAndSignDate(Long userId, LocalDate signDate);

    long countByUserId(Long userId);

    Page<SignInRecord> findByUserIdOrderBySignDateDescIdDesc(Long userId, Pageable pageable);
}
