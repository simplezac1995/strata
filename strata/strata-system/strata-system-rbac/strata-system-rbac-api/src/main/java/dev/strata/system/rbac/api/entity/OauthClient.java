// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "sys_oauth_client")
@Data
@EqualsAndHashCode(callSuper = true)
public class OauthClient extends AuditingEntity {

    @Column(name = "client_id", nullable = false, unique = true, length = 64)
    private String clientId;

    @Column(nullable = false, length = 64)
    private String name;

    /**
     * OAuth2 client secret. Excluded from {@code toString()} (P1-8) so a logged entity dump cannot leak it.
     */
    @ToString.Exclude
    @Column(name = "client_secret", nullable = false, length = 128)
    private String clientSecret;

    @Column(length = 256)
    private String scopes;

    @Column(nullable = false)
    private boolean enabled = true;
}
