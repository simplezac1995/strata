// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.desensitize;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DesensitizeSerializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    class PhoneDesensitization {

        @Test
        void shouldMaskPhoneWithStandardLength() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.phone = "13812345678";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("138****5678");
        }

        @Test
        void shouldMaskShortPhoneCompletely() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.phone = "13812";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("*****");
        }

        @Test
        void shouldKeepNullPhoneAsNull() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.phone = null;

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("\"phone\":null");
        }
    }

    @Nested
    class EmailDesensitization {

        @Test
        void shouldMaskEmailWithFirstCharAndDomain() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.email = "zhangsan@example.com";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("z*******@example.com");
        }

        @Test
        void shouldMaskShortEmailUsername() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.email = "ab@example.com";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("a***@example.com");
        }
    }

    @Nested
    class IdCardDesensitization {

        @Test
        void shouldMaskIdCardWithStandardLength() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.idCard = "110101199001011234";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("110101********1234");
        }

        @Test
        void shouldMaskShortIdCardCompletely() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.idCard = "110101";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("******");
        }
    }

    @Nested
    class BankCardDesensitization {

        @Test
        void shouldMaskBankCardWithSpaces() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.bankCard = "6222021234567890123";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("6222 **** **** *** 0123");
        }
    }

    @Nested
    class NameDesensitization {

        @Test
        void shouldMaskNameWithFirstChar() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.name = "张三丰";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("张**");
        }

        @Test
        void shouldKeepSingleCharName() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.name = "张";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("\"name\":\"张\"");
        }
    }

    @Nested
    class AddressDesensitization {

        @Test
        void shouldMaskAddressWithFirstSixChars() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.address = "北京市朝阳区建国路88号SOHO现代城";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("北京市朝阳区****");
        }

        @Test
        void shouldMaskShortAddressCompletely() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.address = "北京市";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("***");
        }
    }

    @Nested
    class PasswordDesensitization {

        @Test
        void shouldMaskPasswordAsSixStars() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.password = "mySecretPassword123";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("\"password\":\"******\"");
        }
    }

    @Nested
    class CustomDesensitization {

        @Test
        void shouldMaskWithCustomKeepLeftAndRight() throws JsonProcessingException {
            CustomBean bean = new CustomBean();
            bean.custom = "ABCDEFGHIJ";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("AB*****HIJ");
        }
    }

    static class TestBean {
        @JsonSerialize(using = DesensitizeSerializer.class)
        @Desensitize(type = DesensitizeType.PHONE)
        String phone;

        @JsonSerialize(using = DesensitizeSerializer.class)
        @Desensitize(type = DesensitizeType.EMAIL)
        String email;

        @JsonSerialize(using = DesensitizeSerializer.class)
        @Desensitize(type = DesensitizeType.ID_CARD)
        String idCard;

        @JsonSerialize(using = DesensitizeSerializer.class)
        @Desensitize(type = DesensitizeType.BANK_CARD)
        String bankCard;

        @JsonSerialize(using = DesensitizeSerializer.class)
        @Desensitize(type = DesensitizeType.NAME)
        String name;

        @JsonSerialize(using = DesensitizeSerializer.class)
        @Desensitize(type = DesensitizeType.ADDRESS)
        String address;

        @JsonSerialize(using = DesensitizeSerializer.class)
        @Desensitize(type = DesensitizeType.PASSWORD)
        String password;
    }

    static class CustomBean {
        @JsonSerialize(using = DesensitizeSerializer.class)
        @Desensitize(type = DesensitizeType.CUSTOM, keepLeft = 2, keepRight = 3)
        String custom;
    }
}
