// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.system.api.dto.CacheKeyDto;
import dev.strata.system.api.dto.CacheStatusDto;
import dev.strata.system.communication.biz.config.CommunityFeaturesProperties;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheMonitorAppService 单元测试（本地无 Redis 分支）")
class CacheMonitorAppServiceTest {

    @Mock
    private ObjectProvider<StringRedisTemplate> redisProvider;

    private CommunityFeaturesProperties communityFeatures;

    private CacheMonitorAppService service;

    @BeforeEach
    void setUp() {
        when(redisProvider.getIfAvailable()).thenReturn(null);
        communityFeatures = new CommunityFeaturesProperties();
        service = new CacheMonitorAppService(redisProvider, communityFeatures);
    }

    @AfterEach
    void reseedLocalStore() {
        // flush() 在本地 demo 模式下会 clear 并重新播种静态演示数据，恢复测试间一致性
        CommunityFeaturesProperties props = new CommunityFeaturesProperties();
        props.setCacheLocalDemo(true);
        new CacheMonitorAppService(redisProvider, props).flush();
    }

    @Test
    @DisplayName("无 Redis 且开启本地 demo 时状态为 local")
    void status_LocalDemo_ReportsLocal() {
        communityFeatures.setCacheLocalDemo(true);

        CacheStatusDto status = service.status();

        assertThat(status.source()).isEqualTo("local");
        assertThat(status.stub()).isTrue();
    }

    @Test
    @DisplayName("无 Redis 且关闭本地 demo 时状态为 none")
    void status_NoCache_ReportsNone() {
        communityFeatures.setCacheLocalDemo(false);

        CacheStatusDto status = service.status();

        assertThat(status.source()).isEqualTo("none");
        assertThat(status.stub()).isFalse();
    }

    @Test
    @DisplayName("关闭本地 demo 时列出键返回空")
    void listKeys_DemoOff_ReturnsEmpty() {
        communityFeatures.setCacheLocalDemo(false);

        List<CacheKeyDto> keys = service.listKeys("*");

        assertThat(keys).isEmpty();
    }

    @Test
    @DisplayName("本地 demo 按模式过滤键")
    void listKeys_DemoOn_FiltersByPattern() {
        communityFeatures.setCacheLocalDemo(true);

        List<CacheKeyDto> keys = service.listKeys("i18n:*");

        assertThat(keys).extracting(CacheKeyDto::key).containsExactly("i18n:en", "i18n:zh");
    }

    @Test
    @DisplayName("模式为空时默认匹配全部")
    void listKeys_BlankPattern_MatchesAll() {
        communityFeatures.setCacheLocalDemo(true);

        List<CacheKeyDto> keys = service.listKeys(null);

        assertThat(keys).hasSize(5);
    }

    @Test
    @DisplayName("删除空白键抛出 BAD_REQUEST")
    void deleteKey_Blank_Throws() {
        assertThatThrownBy(() -> service.deleteKey("   "))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    @DisplayName("本地 demo 删除键后不再列出")
    void deleteKey_LocalDemo_RemovesKey() {
        communityFeatures.setCacheLocalDemo(true);
        assertThat(service.listKeys("login-lock:*")).hasSize(1);

        service.deleteKey("login-lock:admin");

        assertThat(service.listKeys("login-lock:*")).isEmpty();
    }

    @Test
    @DisplayName("关闭本地 demo 删除键为空操作不抛异常")
    void deleteKey_DemoOff_NoOp() {
        communityFeatures.setCacheLocalDemo(false);

        assertThatCode(() -> service.deleteKey("whatever:key")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("flush 本地 demo 后恢复播种数据")
    void flush_LocalDemo_Reseeds() {
        communityFeatures.setCacheLocalDemo(true);
        service.deleteKey("login-lock:admin");
        assertThat(service.listKeys("login-lock:*")).isEmpty();

        service.flush();

        assertThat(service.listKeys("login-lock:*")).hasSize(1);
    }
}
