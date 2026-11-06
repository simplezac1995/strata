// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("社交登录 State Store（Redis 后端）测试")
class SocialLoginStateStoreRedisTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private SocialLoginStateStore store;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        SocialLoginProperties properties = new SocialLoginProperties();
        properties.setStateStore("redis");
        store = new SocialLoginStateStore(properties, redisTemplate);
    }

    private String captureIssuedPayload(String state) {
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(key.capture(), payload.capture(), eq(Duration.ofMinutes(10)));
        assertThat(key.getValue()).isEqualTo("strata:social:state:" + state);
        return payload.getValue();
    }

    @Test
    @DisplayName("issue 写入带 TTL 的 JSON，consume 原子读取并还原")
    void issueThenConsume_roundTrips() {
        SocialLoginStateStore.Session session = store.issue("keycloak", "https://app.example.com/sso-callback");
        String payload = captureIssuedPayload(session.state());
        when(valueOps.getAndDelete("strata:social:state:" + session.state())).thenReturn(payload);

        SocialLoginStateStore.Entry entry = store.consume(session.state());

        assertThat(entry.codeVerifier()).isEqualTo(session.codeVerifier());
        assertThat(entry.codeChallenge()).isEqualTo(session.codeChallenge());
        assertThat(entry.provider()).isEqualTo("keycloak");
        assertThat(entry.redirectUri()).isEqualTo("https://app.example.com/sso-callback");
    }

    @Test
    @DisplayName("state 不存在（已用过/过期）: UNAUTHORIZED")
    void consumeMissing_rejected() {
        when(valueOps.getAndDelete(anyString())).thenReturn(null);
        assertThatThrownBy(() -> store.consume("unknown"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("过期 state: UNAUTHORIZED")
    void consumeExpired_rejected() {
        String expired = "{\"cv\":\"v\",\"cc\":\"c\",\"p\":\"keycloak\",\"ru\":\"https://app/cb\",\"exp\":"
                + Instant.now().minusSeconds(60).toEpochMilli() + "}";
        when(valueOps.getAndDelete(anyString())).thenReturn(expired);
        assertThatThrownBy(() -> store.consume("state"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("损坏的 payload: UNAUTHORIZED")
    void consumeMalformed_rejected() {
        when(valueOps.getAndDelete(anyString())).thenReturn("not-json");
        assertThatThrownBy(() -> store.consume("state"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("未知的 state-store 值: 启动即失败")
    void unknownBackend_rejected() {
        SocialLoginProperties properties = new SocialLoginProperties();
        properties.setStateStore("memcached");
        assertThatThrownBy(() -> new SocialLoginStateStore(properties, redisTemplate))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("redis 模式没有模板: 启动即失败")
    void redisWithoutTemplate_rejected() {
        SocialLoginProperties properties = new SocialLoginProperties();
        properties.setStateStore("redis");
        assertThatThrownBy(() -> new SocialLoginStateStore(properties, (StringRedisTemplate) null))
                .isInstanceOf(IllegalStateException.class);
    }
}
