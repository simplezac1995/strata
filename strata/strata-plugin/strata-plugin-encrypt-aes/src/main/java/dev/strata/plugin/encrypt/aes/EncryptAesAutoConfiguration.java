// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.encrypt.aes;

import dev.strata.plugin.api.crypto.ApiPayloadCipher;
import java.util.Base64;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
@EnableConfigurationProperties(EncryptAesProperties.class)
@ConditionalOnProperty(prefix = "strata.plugin.encrypt.aes", name = "enabled", havingValue = "true")
public class EncryptAesAutoConfiguration {

    @Bean
    ApiPayloadCipher aesApiPayloadCipher(EncryptAesProperties properties) {
        byte[] key = Base64.getDecoder().decode(properties.getKey());
        return new AesApiPayloadCipher(key);
    }

    @Bean
    FilterRegistrationBean<ApiPayloadDecryptFilter> apiPayloadDecryptFilter(ApiPayloadCipher cipher) {
        FilterRegistrationBean<ApiPayloadDecryptFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ApiPayloadDecryptFilter(cipher));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 15);
        return bean;
    }
}
