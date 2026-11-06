// SPDX-License-Identifier: Apache-2.0
package {{PKG}}.biz.repository;

import {{PKG}}.api.entity.{{E}};
{{REPO_IMPORTS}}import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface {{E}}Repository extends JpaRepository<{{E}}, Long>, JpaSpecificationExecutor<{{E}}> {
{{UNIQUE_REPO_METHODS}}
}
