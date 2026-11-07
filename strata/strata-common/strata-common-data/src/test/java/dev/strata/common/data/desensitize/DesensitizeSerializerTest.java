// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.data.desensitize;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DesensitizeSerializerTest {

    /** 覆盖全部预定义类型的样例对象。 */
    static class Sample {

        @Desensitize(type = DesensitizeType.PHONE)
        public String phone;

        @Desensitize(type = DesensitizeType.EMAIL)
        public String email;

        @Desensitize(type = DesensitizeType.ID_CARD)
        public String idCard;

        @Desensitize(type = DesensitizeType.BANK_CARD)
        public String bankCard;

        @Desensitize(type = DesensitizeType.NAME)
        public String name;

        @Desensitize(type = DesensitizeType.ADDRESS)
        public String address;

        @Desensitize(type = DesensitizeType.PASSWORD)
        public String password;

        @Desensitize(type = DesensitizeType.FULL_MASK)
        public String secret;

        @Desensitize(type = DesensitizeType.API_KEY)
        public String apiKey;

        @Desensitize(type = DesensitizeType.SECRET_KEY)
        public String secretKey;

        @Desensitize(type = DesensitizeType.IP_ADDRESS)
        public String ip;

        @Desensitize(type = DesensitizeType.MAC_ADDRESS)
        public String mac;

        @Desensitize(type = DesensitizeType.CUSTOM, keepLeft = 2, keepRight = 2, maskChar = '#')
        public String custom;

        @Desensitize(type = DesensitizeType.PHONE)
        public String emptyValue;

        @Desensitize(type = DesensitizeType.PHONE)
        public String nullValue;
    }

    private final ObjectMapper mapper = new ObjectMapper();

    private Map<String, Object> serialize(Sample sample) throws Exception {
        String json = mapper.writeValueAsString(sample);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = mapper.readValue(json, Map.class);
        return map;
    }

    @Test
    void masksPredefinedTypes() throws Exception {
        Sample s = new Sample();
        s.phone = "13812345678";
        s.email = "alice@example.com";
        s.idCard = "110101199001011234";
        s.bankCard = "6222123456789012";
        s.name = "张三丰";
        s.address = "北京市朝阳区望京街道123号";
        s.password = "secret123";
        s.secret = "abc";
        s.apiKey = "abcdefghijklmnopqrst";
        s.secretKey = "abcdEFGH";
        s.ip = "192.168.1.100";
        s.mac = "00:1A:2B:3C:4D:5E";
        s.custom = "ABCDEFGH";

        Map<String, Object> out = serialize(s);

        assertThat(out.get("phone")).isEqualTo("138****5678");
        assertThat(out.get("email")).isEqualTo("a****@example.com");
        assertThat(out.get("idCard")).isEqualTo("110101********1234");
        assertThat(out.get("bankCard")).isEqualTo("6222 **** **** 9012");
        assertThat(out.get("name")).isEqualTo("张**");
        assertThat(out.get("address").toString()).startsWith("北京市朝阳区");
        assertThat(out.get("password")).isEqualTo("******");
        assertThat(out.get("secret")).isEqualTo("***");
        assertThat(out.get("apiKey")).isEqualTo("abcdefgh********qrst");
        assertThat(out.get("secretKey")).isEqualTo("abcd****");
        assertThat(out.get("ip")).isEqualTo("192.168.**.**");
        assertThat(out.get("mac")).isEqualTo("00:1A:2B:**:**:**");
        assertThat(out.get("custom")).isEqualTo("AB####GH");
    }

    @Test
    void passesThroughNullOrEmpty() throws Exception {
        Sample s = new Sample();
        s.emptyValue = "";

        Map<String, Object> out = serialize(s);

        assertThat(out.get("nullValue")).isNull();
        assertThat(out.get("emptyValue")).isEqualTo("");
    }

    @Test
    void masksShortEmailUsername() throws Exception {
        Sample s = new Sample();
        s.email = "ab@x.com";

        Map<String, Object> out = serialize(s);

        assertThat(out.get("email")).isEqualTo("a***@x.com");
    }

    @Test
    void singleCharNameUnchanged() throws Exception {
        Sample s = new Sample();
        s.name = "张";

        Map<String, Object> out = serialize(s);

        assertThat(out.get("name")).isEqualTo("张");
    }

    @Test
    void customTooShortMaskAll() throws Exception {
        Sample s = new Sample();
        s.custom = "AB";

        Map<String, Object> out = serialize(s);

        assertThat(out.get("custom")).isEqualTo("##");
    }

    @Test
    void invalidIpAndMacFallbackToMaskAll() throws Exception {
        Sample s = new Sample();
        s.ip = "not-an-ip";
        s.mac = "not-a-mac";

        Map<String, Object> out = serialize(s);

        assertThat(out.get("ip")).isEqualTo("*********");
        assertThat(out.get("mac")).isEqualTo("*********");
    }

    @Test
    void shortValuesFallbackToMaskAll() throws Exception {
        Sample s = new Sample();
        s.phone = "12345"; // < 7 位
        s.email = "@x.com"; // atIndex <= 1
        s.idCard = "12345"; // < 10 位
        s.bankCard = "1234567"; // 去空白后 < 8 位
        s.address = "短地址"; // <= 6 字
        s.apiKey = "shortkey"; // <= 12 位
        s.secretKey = "abcd"; // <= 4 位

        Map<String, Object> out = serialize(s);

        assertThat(out.get("phone")).isEqualTo("*****");
        assertThat(out.get("email")).isEqualTo("******");
        assertThat(out.get("idCard")).isEqualTo("*****");
        assertThat(out.get("bankCard")).isEqualTo("*******");
        assertThat(out.get("address")).isEqualTo("***");
        assertThat(out.get("apiKey")).isEqualTo("********");
        assertThat(out.get("secretKey")).isEqualTo("****");
    }

    @Test
    void macWithDashSeparatorPreservesDashes() throws Exception {
        Sample s = new Sample();
        s.mac = "00-1A-2B-3C-4D-5E";

        Map<String, Object> out = serialize(s);

        assertThat(out.get("mac")).isEqualTo("00-1A-2B-**-**-**");
    }
}
