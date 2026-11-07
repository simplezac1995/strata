// SPDX-License-Identifier: Apache-2.0
package {{PKG}}.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
{{ENTITY_IMPORTS}}import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "{{TABLE}}")
@Data
@EqualsAndHashCode(callSuper = true)
public class {{E}} extends AuditingEntity {

{{ENTITY_FIELDS}}
}
