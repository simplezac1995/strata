// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import dev.strata.system.ops.api.entity.ApiCallStat;
import dev.strata.system.ops.biz.repository.ApiCallStatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiCallStatRecorderTest {

    @Mock
    private ApiCallStatRepository repository;

    @InjectMocks
    private ApiCallStatRecorder recorder;

    @Test
    void record_SavesRow() {
        recorder.record("/api/v1/x", "GET", 200, 12L, "127.0.0.1");

        verify(repository).save(any(ApiCallStat.class));
    }

    @Test
    void record_RepositoryThrows_Swallows() {
        doThrow(new RuntimeException("db down")).when(repository).save(any(ApiCallStat.class));

        recorder.record("/api/v1/x", "GET", 500, 9L, "127.0.0.1");
        // no exception escapes
    }
}
