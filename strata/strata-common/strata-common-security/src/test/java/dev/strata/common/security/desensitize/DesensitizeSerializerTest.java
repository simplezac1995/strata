// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.desensitize;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DesensitizeSerializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("手机号脱敏")
    class PhoneDesensitization {

        @Test
        @DisplayName("正常手机号应保留前3后4")
        void shouldMaskPhoneWithStandardLength() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.phone = "13812345678";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("138****5678");
        }

        @Test
        @DisplayName("短于7位的手机号应全部脱敏")
        void shouldMaskShortPhoneCompletely() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.phone = "13812";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("*****");
        }

        @Test
        @DisplayName("null 手机号应保持 null")
        void shouldKeepNullPhoneAsNull() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.phone = null;

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("\"phone\":null");
        }
    }

    @Nested
    @DisplayName("邮箱脱敏")
    class EmailDesensitization {

        @Test
        @DisplayName("正常邮箱应保留首字母和域名")
        void shouldMaskEmailWithFirstCharAndDomain() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.email = "zhangsan@example.com";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("z*******@example.com");
        }

        @Test
        @DisplayName("短用户名邮箱应保留首字母并脱敏3位")
        void shouldMaskShortEmailUsername() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.email = "ab@example.com";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("a***@example.com");
        }
    }

    @Nested
    @DisplayName("身份证脱敏")
    class IdCardDesensitization {

        @Test
        @DisplayName("正常身份证应保留前6后4")
        void shouldMaskIdCardWithStandardLength() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.idCard = "110101199001011234";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("110101********1234");
        }

        @Test
        @DisplayName("短于10位的身份证应全部脱敏")
        void shouldMaskShortIdCardCompletely() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.idCard = "110101";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("******");
        }
    }

    @Nested
    @DisplayName("银行卡脱敏")
    class BankCardDesensitization {

        @Test
        @DisplayName("正常银行卡应保留前4后4并带空格分隔")
        void shouldMaskBankCardWithSpaces() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.bankCard = "6222021234567890123";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("6222 **** **** *** 0123");
        }
    }

    @Nested
    @DisplayName("姓名脱敏")
    class NameDesensitization {

        @Test
        @DisplayName("多字姓名应保留首字")
        void shouldMaskNameWithFirstChar() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.name = "张三丰";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("张**");
        }

        @Test
        @DisplayName("单字姓名应保持不变")
        void shouldKeepSingleCharName() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.name = "张";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("\"name\":\"张\"");
        }
    }

    @Nested
    @DisplayName("地址脱敏")
    class AddressDesensitization {

        @Test
        @DisplayName("长地址应保留前6位")
        void shouldMaskAddressWithFirstSixChars() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.address = "北京市朝阳区建国路88号SOHO现代城";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("北京市朝阳区****");
        }

        @Test
        @DisplayName("短于等于6位的地址应全部脱敏")
        void shouldMaskShortAddressCompletely() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.address = "北京市";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("***");
        }
    }

    @Nested
    @DisplayName("密码脱敏")
    class PasswordDesensitization {

        @Test
        @DisplayName("密码应固定显示6个星号")
        void shouldMaskPasswordAsSixStars() throws JsonProcessingException {
            TestBean bean = new TestBean();
            bean.password = "mySecretPassword123";

            String json = objectMapper.writeValueAsString(bean);

            assertThat(json).contains("\"password\":\"******\"");
        }
    }

    @Nested
    @DisplayName("自定义脱敏")
    class CustomDesensitization {

        @Test
        @DisplayName("自定义保留前2后3")
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
