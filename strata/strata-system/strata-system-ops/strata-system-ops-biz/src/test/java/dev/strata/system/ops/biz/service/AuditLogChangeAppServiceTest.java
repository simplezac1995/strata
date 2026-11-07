// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.AuditLogChangeDto;
import dev.strata.system.ops.api.entity.AuditLogChange;
import dev.strata.system.ops.biz.mapper.AuditLogChangeMapper;
import dev.strata.system.ops.biz.repository.AuditLogChangeRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "varargs"})
class AuditLogChangeAppServiceTest {

    @Mock
    private AuditLogChangeRepository auditLogChangeRepository;

    @Mock
    private AuditLogChangeMapper auditLogChangeMapper;

    @InjectMocks
    private AuditLogChangeAppService service;

    @Test
    void page_FiltersByEntity() {
        AuditLogChange row = new AuditLogChange();
        when(auditLogChangeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row)));
        when(auditLogChangeMapper.toDto(any(AuditLogChange.class)))
                .thenReturn(new AuditLogChangeDto("1", "7", "RiskRule", "5", "name", "a", "b", null));

        PageResult<AuditLogChangeDto> result = service.page("RiskRule", "5", 0, 10);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void rollback_Existing_ReturnsRow() {
        AuditLogChange row = new AuditLogChange();
        row.setEntityName("RiskRule");
        row.setFieldName("name");
        when(auditLogChangeRepository.findById(5L)).thenReturn(Optional.of(row));
        when(auditLogChangeMapper.toDto(row))
                .thenReturn(new AuditLogChangeDto("5", null, "RiskRule", null, "name", "a", "b", null));

        AuditLogChangeDto dto = service.rollback(5L);

        assertThat(dto.entityName()).isEqualTo("RiskRule");
    }

    @Test
    void rollback_Missing_ThrowsNotFound() {
        when(auditLogChangeRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rollback(9L)).isInstanceOf(BusinessException.class);
    }
}
