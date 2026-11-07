// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.desensitize;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Branch arms of {@link DesensitizeSerializer} not in {@link DesensitizeSerializerTest}: empty-string
 * serialization, the {@code atIndex<=1} email arm, short bank card, custom mask shorter than keepLeft+keepRight,
 * a zero-length {@code repeat}, and the {@code createContextual} fallbacks.
 */
class DesensitizeSerializerBranchTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void emptyStringPassesThrough() throws JsonProcessingException {
        EdgeBean bean = new EdgeBean();
        bean.phone = "";
        assertThat(mapper.writeValueAsString(bean)).contains("\"phone\":\"\"");
    }

    @Test
    void emailAtIndexLeqOneMasked() throws JsonProcessingException {
        EdgeBean bean = new EdgeBean();
        bean.email = "@example.com";
        String json = mapper.writeValueAsString(bean);
        assertThat(json).doesNotContain("@example.com");
        assertThat(json).contains("\"email\":\"************\"");
    }

    @Test
    void shortBankCardMasked() throws JsonProcessingException {
        EdgeBean bean = new EdgeBean();
        bean.bankCard = "12 345";
        String json = mapper.writeValueAsString(bean);
        assertThat(json).contains("*****");
    }

    @Test
    void customShorterThanKeepsMasked() throws JsonProcessingException {
        CustomEdgeBean bean = new CustomEdgeBean();
        bean.custom = "ABC"; // keepLeft=4 keepRight=4, length 3 <= 8
        String json = mapper.writeValueAsString(bean);
        assertThat(json).contains("\"custom\":\"***\"");
    }

    @Test
    void sevenDigitPhoneZeroRepeat() throws JsonProcessingException {
        EdgeBean bean = new EdgeBean();
        bean.phone = "1234567"; // length 7 -> repeat(0)
        String json = mapper.writeValueAsString(bean);
        assertThat(json).isEqualTo("{\"phone\":\"1234567\",\"email\":null,\"bankCard\":null,\"plain\":null}");
    }

    @Test
    void contextualWithoutAnnotationFallsBack() throws JsonProcessingException {
        EdgeBean bean = new EdgeBean();
        bean.plain = "visible";
        String json = mapper.writeValueAsString(bean);
        assertThat(json).contains("\"plain\":\"visible\"");
    }

    @Test
    void contextualNullProperty() throws Exception {
        SerializerProvider prov = mapper.getSerializerProviderInstance();
        JsonSerializer<?> s = new DesensitizeSerializer().createContextual(prov, null);
        assertThat(s).isNotNull();
    }

    static class EdgeBean {
        @JsonSerialize(using = DesensitizeSerializer.class)
        @Desensitize(type = DesensitizeType.PHONE)
        String phone;

        @JsonSerialize(using = DesensitizeSerializer.class)
        @Desensitize(type = DesensitizeType.EMAIL)
        String email;

        @JsonSerialize(using = DesensitizeSerializer.class)
        @Desensitize(type = DesensitizeType.BANK_CARD)
        String bankCard;

        // 故意不带 @Desensitize，走 createContextual 回退臂
        @JsonSerialize(using = DesensitizeSerializer.class)
        String plain;
    }

    static class CustomEdgeBean {
        @JsonSerialize(using = DesensitizeSerializer.class)
        @Desensitize(type = DesensitizeType.CUSTOM, keepLeft = 4, keepRight = 4)
        String custom;
    }
}
