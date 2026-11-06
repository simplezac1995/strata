// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IdempotencyProperties 配置规范化")
class IdempotencyPropertiesTest {

    @Test
    @DisplayName("缺省值全部回落到安全默认")
    void nullsAndInvalidValuesFallBackToDefaults() {
        IdempotencyProperties props =
                new IdempotencyProperties(true, false, null, null, null, null, null, 0, false, null);

        assertThat(props.enabled()).isTrue();
        assertThat(props.required()).isFalse();
        assertThat(props.headerName()).isEqualTo("Idempotency-Key");
        assertThat(props.pathPatterns()).containsExactly("/api/v1/**", "/v1/**");
        assertThat(props.requiredPathPatterns()).isEmpty();
        assertThat(props.methods()).containsExactly("POST", "PUT", "PATCH", "DELETE");
        assertThat(props.retention()).isEqualTo(Duration.ofHours(24));
        assertThat(props.maxBodyBytes()).isEqualTo(65536);
        assertThat(props.purgeEnabled()).isFalse();
        assertThat(props.purgeInterval()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("空白 headerName 与空方法列表也回落到默认")
    void blankHeaderAndEmptyMethodsFallBack() {
        IdempotencyProperties props = new IdempotencyProperties(
                true, false, "   ", List.of(), List.of(), List.of(), Duration.ZERO, -5, true, Duration.ofSeconds(-1));

        assertThat(props.headerName()).isEqualTo("Idempotency-Key");
        assertThat(props.pathPatterns()).containsExactly("/api/v1/**", "/v1/**");
        assertThat(props.methods()).containsExactly("POST", "PUT", "PATCH", "DELETE");
        assertThat(props.retention()).isEqualTo(Duration.ofHours(24));
        assertThat(props.maxBodyBytes()).isEqualTo(65536);
        assertThat(props.purgeInterval()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("合法自定义值原样保留")
    void validValuesArePreserved() {
        IdempotencyProperties props = new IdempotencyProperties(
                false,
                true,
                "X-Idem",
                List.of("/custom/**"),
                List.of("/custom/pay"),
                List.of("POST"),
                Duration.ofHours(1),
                1024,
                true,
                Duration.ofMinutes(5));

        assertThat(props.headerName()).isEqualTo("X-Idem");
        assertThat(props.pathPatterns()).containsExactly("/custom/**");
        assertThat(props.requiredPathPatterns()).containsExactly("/custom/pay");
        assertThat(props.methods()).containsExactly("POST");
        assertThat(props.retention()).isEqualTo(Duration.ofHours(1));
        assertThat(props.maxBodyBytes()).isEqualTo(1024);
        assertThat(props.purgeInterval()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("键长度上限常量固定为 128")
    void maxKeyLengthConstant() {
        assertThat(IdempotencyProperties.MAX_KEY_LENGTH).isEqualTo(128);
    }
}
