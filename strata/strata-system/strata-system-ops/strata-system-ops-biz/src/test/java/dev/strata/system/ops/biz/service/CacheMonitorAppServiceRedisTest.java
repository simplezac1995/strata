// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.system.api.dto.CacheKeyDto;
import dev.strata.system.api.dto.CacheStatusDto;
import dev.strata.system.communication.biz.config.CommunityFeaturesProperties;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 分支：深链 mock RedisConnectionFactory → RedisConnection → serverCommands。
 * 与本地 demo 分支的既有测试 {@link CacheMonitorAppServiceTest} 互补。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CacheMonitorAppService 单元测试（Redis 分支）")
class CacheMonitorAppServiceRedisTest {

    @Mock
    private ObjectProvider<StringRedisTemplate> redisProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection connection;

    @Mock
    private RedisServerCommands serverCommands;

    private CommunityFeaturesProperties communityFeatures;
    private CacheMonitorAppService service;

    @BeforeEach
    void setUp() {
        communityFeatures = new CommunityFeaturesProperties();
        when(redisProvider.getIfAvailable()).thenReturn(redisTemplate);
        service = new CacheMonitorAppService(redisProvider, communityFeatures);
    }

    private void stubConnection() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.serverCommands()).thenReturn(serverCommands);
    }

    @Nested
    @DisplayName("status - Redis 状态")
    class StatusTests {

        @Test
        @DisplayName("Redis 可用时返回真实统计")
        void status_RedisUp_ReturnsStats() {
            stubConnection();
            Properties info = new Properties();
            info.setProperty("redis_version", "7.2.0");
            info.setProperty("used_memory_human", "2.5M");
            info.setProperty("connected_clients", "3");
            info.setProperty("keyspace_hits", "10");
            info.setProperty("keyspace_misses", "0");
            when(serverCommands.info()).thenReturn(info);
            when(serverCommands.dbSize()).thenReturn(42L);

            CacheStatusDto status = service.status();

            assertThat(status.source()).isEqualTo("redis");
            assertThat(status.stub()).isFalse();
            assertThat(status.redisVersion()).isEqualTo("7.2.0");
            assertThat(status.usedMemory()).isEqualTo("2.5M");
            assertThat(status.totalKeys()).isEqualTo(42L);
            assertThat(status.connectedClients()).isEqualTo(3L);
            assertThat(status.hitRate()).isEqualTo(1.0);
            verify(connection).close();
        }

        @Test
        @DisplayName("Redis 异常时降级为 redis/unavailable")
        void status_RedisDown_Degrades() {
            when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
            when(connectionFactory.getConnection()).thenThrow(new RuntimeException("conn refused"));

            CacheStatusDto status = service.status();

            assertThat(status.source()).isEqualTo("redis");
            assertThat(status.stub()).isFalse();
            assertThat(status.redisVersion()).isNull();
        }

        @Test
        @DisplayName("info 缺字段时 hitRate 为 null、clients 为 null")
        void status_MissingFields_NullSafe() {
            stubConnection();
            Properties info = new Properties();
            // 全部留空 → parseLong 返回 null，hits+misses=0 → hitRate=null
            when(serverCommands.info()).thenReturn(info);
            when(serverCommands.dbSize()).thenReturn(0L);

            CacheStatusDto status = service.status();

            assertThat(status.source()).isEqualTo("redis");
            assertThat(status.connectedClients()).isNull();
            assertThat(status.hitRate()).isNull();
            verify(connection).close();
        }
    }

    @Nested
    @DisplayName("listKeys - Redis 键扫描")
    class ListKeysTests {

        @Test
        @DisplayName("扫描返回键并附带类型与 TTL")
        @SuppressWarnings("unchecked")
        void listKeys_Redis_ReturnsKeys() {
            Cursor<String> cursor = org.mockito.Mockito.mock(Cursor.class);
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
            when(cursor.hasNext()).thenReturn(true, true, false);
            when(cursor.next()).thenReturn("session:1", "session:2");
            when(redisTemplate.type("session:1")).thenReturn(DataType.STRING);
            when(redisTemplate.type("session:2")).thenReturn(null);
            when(redisTemplate.getExpire("session:1")).thenReturn(-1L);
            when(redisTemplate.getExpire("session:2")).thenReturn(300L);

            List<CacheKeyDto> keys = service.listKeys("session:*");

            assertThat(keys).hasSize(2);
            assertThat(keys.get(0).key()).isEqualTo("session:1");
            assertThat(keys.get(0).type()).isEqualTo("string");
            // type==null → "unknown"
            assertThat(keys.get(1).type()).isEqualTo("unknown");
        }

        @Test
        @DisplayName("模式为空时默认匹配全部")
        @SuppressWarnings("unchecked")
        void listKeys_BlankPattern_MatchesAll() {
            Cursor<String> cursor = org.mockito.Mockito.mock(Cursor.class);
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
            when(cursor.hasNext()).thenReturn(false);

            List<CacheKeyDto> keys = service.listKeys("  ");

            assertThat(keys).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteKey / flush - Redis 删除")
    class DeleteTests {

        @Test
        @DisplayName("Redis 模式下按 key 删除")
        void deleteKey_Redis_DeletesByKey() {
            service.deleteKey("some:key");

            verify(redisTemplate).delete("some:key");
        }

        @Test
        @DisplayName("Redis 模式下 flush 扫描后批量删除")
        @SuppressWarnings("unchecked")
        void flush_Redis_DeletesByPattern() {
            Cursor<String> cursor = org.mockito.Mockito.mock(Cursor.class);
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
            when(cursor.hasNext()).thenReturn(true, true, false);
            when(cursor.next()).thenReturn("k1", "k2");

            service.flush();

            verify(redisTemplate).delete(anyCollection());
        }

        @Test
        @DisplayName("Redis 模式下 flush 无键时不调用批量删除")
        @SuppressWarnings("unchecked")
        void flush_RedisNoKeys_NoBulkDelete() {
            Cursor<String> cursor = org.mockito.Mockito.mock(Cursor.class);
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
            when(cursor.hasNext()).thenReturn(false);

            service.flush();

            verify(redisTemplate, org.mockito.Mockito.never()).delete(anyCollection());
        }
    }
}
