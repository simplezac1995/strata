// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.strata.system.api.dto.OpsHealthDto;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class OpsHealthAppServiceTest {

    @Mock
    private ObjectProvider<DataSource> dataSourceProvider;

    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    private OpsHealthAppService service;

    @BeforeEach
    void setUp() {
        service = new OpsHealthAppService(dataSourceProvider, redisTemplateProvider);
    }

    @Test
    void check_DbUp() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        when(dataSourceProvider.getIfAvailable()).thenReturn(dataSource);
        when(redisTemplateProvider.getIfAvailable()).thenReturn(null);

        OpsHealthDto result = service.check();

        assertThat(result.db().status()).isEqualTo("up");
        assertThat(result.redis().status()).isEqualTo("unknown");
        assertThat(result.uptimeMs()).isGreaterThanOrEqualTo(0L);
        assertThat(result.disk().totalGb()).isGreaterThan(0d);
        assertThat(result.memory().totalMb()).isGreaterThan(0L);
    }

    @Test
    void check_DbUnknownWhenAbsent() {
        when(dataSourceProvider.getIfAvailable()).thenReturn(null);
        when(redisTemplateProvider.getIfAvailable()).thenReturn(null);

        OpsHealthDto result = service.check();

        assertThat(result.db().status()).isEqualTo("unknown");
    }

    @Test
    void check_DbDownOnError() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new java.sql.SQLException("boom"));
        when(dataSourceProvider.getIfAvailable()).thenReturn(dataSource);
        when(redisTemplateProvider.getIfAvailable()).thenReturn(null);

        OpsHealthDto result = service.check();

        assertThat(result.db().status()).isEqualTo("down");
    }

    @Test
    void check_RedisUp() {
        when(dataSourceProvider.getIfAvailable()).thenReturn(null);

        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        lenient().when(redis.getConnectionFactory()).thenReturn(factory);
        lenient().when(factory.getConnection()).thenReturn(connection);
        lenient().when(connection.ping()).thenReturn("PONG");
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redis);

        OpsHealthDto result = service.check();

        assertThat(result.redis().status()).isEqualTo("up");
    }
}
