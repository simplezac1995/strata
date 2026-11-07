// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.repository;

import dev.strata.system.api.entity.DemoPayment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemoPaymentRepository extends JpaRepository<DemoPayment, Long> {

    Optional<DemoPayment> findBySessionId(String sessionId);

    List<DemoPayment> findAllByOrderByIdDesc();
}
