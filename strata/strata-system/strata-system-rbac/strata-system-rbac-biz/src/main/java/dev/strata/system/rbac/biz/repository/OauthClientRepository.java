// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.repository;

import dev.strata.system.rbac.api.entity.OauthClient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OauthClientRepository extends JpaRepository<OauthClient, Long> {

    List<OauthClient> findAllByOrderByIdAsc();

    boolean existsByClientId(String clientId);
}
