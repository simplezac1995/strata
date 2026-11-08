// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import dev.strata.system.api.dto.ApiStatsDto;
import dev.strata.system.ops.biz.repository.ApiCallStatRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiStatsAppServiceTest {

    @Mock
    private ApiCallStatRepository repository;

    @InjectMocks
    private ApiStatsAppService service;

    @Test
    void stats_EmptyWindow_ReturnsZero() {
        when(repository.totalsSince(any())).thenReturn(new Object[] {0L, null, null});
        when(repository.perPathSince(any())).thenReturn(List.of());

        ApiStatsDto result = service.stats(7);

        assertThat(result.totalCalls()).isZero();
        assertThat(result.avgDurationMs()).isZero();
        assertThat(result.errorRate()).isZero();
        assertThat(result.byPath()).isEmpty();
    }

    @Test
    void stats_PopulatedWindow_Aggregates() {
        when(repository.totalsSince(any())).thenReturn(new Object[] {100L, 12.5d, 4L});
        when(repository.perPathSince(any()))
                .thenReturn(List.of(
                        new Object[] {"/api/v1/player/activities", 80L, 10.0d, 2L},
                        new Object[] {"/api/v1/ops/backups/create", 20L, 22.0d, 2L}));

        ApiStatsDto result = service.stats(7);

        assertThat(result.totalCalls()).isEqualTo(100L);
        assertThat(result.avgDurationMs()).isEqualTo(12.5d);
        assertThat(result.errorRate()).isEqualTo(0.04d);
        assertThat(result.byPath()).hasSize(2);
        assertThat(result.byPath().get(0).path()).isEqualTo("/api/v1/player/activities");
        assertThat(result.byPath().get(0).errorRate()).isEqualTo(0.025d);
        assertThat(result.byPath().get(1).errorRate()).isEqualTo(0.10d);
    }
}
