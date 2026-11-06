// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcIdempotencyStore 幂等存取与重复分类")
@SuppressWarnings({"unchecked", "varargs"})
class JdbcIdempotencyStoreTest {

    @Mock
    private JdbcTemplate jdbc;

    private JdbcIdempotencyStore store;
    private IdempotencyRequest request;

    @BeforeEach
    void setUp() {
        store = new JdbcIdempotencyStore(jdbc);
        request = new IdempotencyRequest(
                "user:1", "k1", "POST", "/api/x", "fp1", Instant.now().plus(1, ChronoUnit.HOURS));
    }

    @Test
    @DisplayName("键空闲时插入 IN_PROGRESS 并返回 Claimed")
    void claimInsertsAndReturnsClaimed() {
        IdempotencyClaim claim = store.claim(request);

        assertThat(claim).isInstanceOf(IdempotencyClaim.Claimed.class);
        assertThat(((IdempotencyClaim.Claimed) claim).id()).isPositive();

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(contains("INSERT"), args.capture());
        Object[] values = args.getValue();
        assertThat(values[1]).isEqualTo("user:1");
        assertThat(values[2]).isEqualTo("k1");
        assertThat(values[5]).isEqualTo("fp1");
        assertThat(values[6]).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("重复键且已完成、指纹一致时返回 Replay 缓存响应")
    void duplicateCompletedReturnsReplay() {
        stubDuplicateInsert();
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any()))
                .thenAnswer(inv -> mappedRow(inv, "fp1", "COMPLETED", 200, "application/json", "{\"ok\":true}"));

        IdempotencyClaim claim = store.claim(request);

        assertThat(claim).isInstanceOf(IdempotencyClaim.Replay.class);
        StoredResponse response = ((IdempotencyClaim.Replay) claim).response();
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"ok\":true}");
    }

    @Test
    @DisplayName("重复键但指纹不同（同键不同负载）返回 Mismatch")
    void duplicateDifferentFingerprintReturnsMismatch() {
        stubDuplicateInsert();
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any()))
                .thenAnswer(inv -> mappedRow(inv, "someone-else", "COMPLETED", 200, "application/json", "{}"));

        assertThat(store.claim(request)).isInstanceOf(IdempotencyClaim.Mismatch.class);
    }

    @Test
    @DisplayName("重复键仍在处理中返回 InProgress")
    void duplicateInProgressReturnsInProgress() {
        stubDuplicateInsert();
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any()))
                .thenAnswer(inv -> mappedRow(inv, "fp1", "IN_PROGRESS", null, null, null));

        assertThat(store.claim(request)).isInstanceOf(IdempotencyClaim.InProgress.class);
    }

    @Test
    @DisplayName("已完成但响应状态缺失时保守返回 InProgress")
    void completedWithoutStatusReturnsInProgress() {
        stubDuplicateInsert();
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any()))
                .thenAnswer(inv -> mappedRow(inv, "fp1", "COMPLETED", null, null, null));

        assertThat(store.claim(request)).isInstanceOf(IdempotencyClaim.InProgress.class);
    }

    @Test
    @DisplayName("重复后行连续两次消失（被并发释放/清理）返回 InProgress，由调用方有界重试")
    void duplicateRowVanishedTwiceReturnsInProgress() {
        stubDuplicateInsert();
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any())).thenReturn(List.of());

        assertThat(store.claim(request)).isInstanceOf(IdempotencyClaim.InProgress.class);
    }

    @Test
    @DisplayName("首次读取行消失、第二次读到已完成记录时返回 Replay")
    void vanishedThenCompletedReturnsReplay() {
        stubDuplicateInsert();
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any()))
                .thenReturn(List.of())
                .thenAnswer(inv -> mappedRow(inv, "fp1", "COMPLETED", 204, "application/json", ""));

        IdempotencyClaim claim = store.claim(request);

        assertThat(claim).isInstanceOf(IdempotencyClaim.Replay.class);
        assertThat(((IdempotencyClaim.Replay) claim).response().status()).isEqualTo(204);
    }

    @Test
    @DisplayName("complete 更新为 COMPLETED，影响 1 行")
    void completeUpdatesRecord() {
        StoredResponse response = new StoredResponse(201, "application/json", "{\"id\":9}");
        when(jdbc.update(contains("UPDATE"), any(Object[].class))).thenReturn(1);

        assertThatCode(() -> store.complete(99L, response)).doesNotThrowAnyException();

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(contains("UPDATE"), args.capture());
        assertThat(args.getValue()[0]).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("complete 时记录已被并发清理（0 行）也不抛异常")
    void completeZeroRowsDoesNotThrow() {
        StoredResponse response = new StoredResponse(201, "application/json", "{}");
        when(jdbc.update(contains("UPDATE"), any(Object[].class))).thenReturn(0);

        assertThatCode(() -> store.complete(99L, response)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("release 仅删除未完成的占用")
    void releaseDeletesInProgressClaim() {
        store.release(7L);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(contains("WHERE id"), args.capture());
        assertThat(args.getValue()[0]).isEqualTo(7L);
        assertThat(args.getValue()[1]).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("purgeExpired 返回删除行数")
    void purgeReturnsDeletedCount() {
        when(jdbc.update(contains("expires_at"), any(Object[].class))).thenReturn(4);

        assertThat(store.purgeExpired(Instant.now())).isEqualTo(4);
    }

    private void stubDuplicateInsert() {
        when(jdbc.update(contains("INSERT"), any(Object[].class)))
                .thenThrow(new DuplicateKeyException("duplicate key"));
    }

    /** Builds a {@code Stored} row through the production RowMapper from a mocked ResultSet. */
    @SuppressWarnings("unchecked")
    private List<?> mappedRow(
            InvocationOnMock invocation,
            String fingerprint,
            String status,
            Integer responseStatus,
            String contentType,
            String body)
            throws Exception {
        RowMapper<Object> mapper = invocation.getArgument(1);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("request_fingerprint")).thenReturn(fingerprint);
        when(rs.getString("status")).thenReturn(status);
        when(rs.getObject("response_status")).thenReturn(responseStatus);
        if (responseStatus != null) {
            when(rs.getInt("response_status")).thenReturn(responseStatus);
        }
        when(rs.getString("response_content_type")).thenReturn(contentType);
        when(rs.getString("response_body")).thenReturn(body);
        return List.of(mapper.mapRow(rs, 0));
    }
}
