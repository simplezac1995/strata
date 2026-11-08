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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
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
    void status_LocalDemo_ReportsLocal() {
        communityFeatures.setCacheLocalDemo(true);

        CacheStatusDto status = service.status();

        assertThat(status.source()).isEqualTo("local");
        assertThat(status.stub()).isTrue();
    }

    @Test
    void status_NoCache_ReportsNone() {
        communityFeatures.setCacheLocalDemo(false);

        CacheStatusDto status = service.status();

        assertThat(status.source()).isEqualTo("none");
        assertThat(status.stub()).isFalse();
    }

    @Test
    void listKeys_DemoOff_ReturnsEmpty() {
        communityFeatures.setCacheLocalDemo(false);

        List<CacheKeyDto> keys = service.listKeys("*");

        assertThat(keys).isEmpty();
    }

    @Test
    void listKeys_DemoOn_FiltersByPattern() {
        communityFeatures.setCacheLocalDemo(true);

        List<CacheKeyDto> keys = service.listKeys("i18n:*");

        assertThat(keys).extracting(CacheKeyDto::key).containsExactly("i18n:en", "i18n:zh");
    }

    @Test
    void listKeys_BlankPattern_MatchesAll() {
        communityFeatures.setCacheLocalDemo(true);

        List<CacheKeyDto> keys = service.listKeys(null);

        assertThat(keys).hasSize(5);
    }

    @Test
    void deleteKey_Blank_Throws() {
        assertThatThrownBy(() -> service.deleteKey("   "))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void deleteKey_LocalDemo_RemovesKey() {
        communityFeatures.setCacheLocalDemo(true);
        assertThat(service.listKeys("login-lock:*")).hasSize(1);

        service.deleteKey("login-lock:admin");

        assertThat(service.listKeys("login-lock:*")).isEmpty();
    }

    @Test
    void deleteKey_DemoOff_NoOp() {
        communityFeatures.setCacheLocalDemo(false);

        assertThatCode(() -> service.deleteKey("whatever:key")).doesNotThrowAnyException();
    }

    @Test
    void flush_LocalDemo_Reseeds() {
        communityFeatures.setCacheLocalDemo(true);
        service.deleteKey("login-lock:admin");
        assertThat(service.listKeys("login-lock:*")).isEmpty();

        service.flush();

        assertThat(service.listKeys("login-lock:*")).hasSize(1);
    }
}
