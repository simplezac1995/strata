// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.saml;

import dev.strata.system.rbac.biz.saml.support.OneTimeTokenStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Registers {@link SamlProperties} plus the two one-time stores used by the redirect handshake. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SamlProperties.class)
public class SamlConfiguration {

    @Bean
    public OneTimeTokenStore samlRelayStateStore(
            SamlProperties properties, ObjectProvider<StringRedisTemplate> redisTemplate) {
        return new OneTimeTokenStore(
                "saml relay state",
                properties.getStateStore(),
                redisTemplate.getIfAvailable(),
                "strata:saml:relay:",
                600);
    }

    @Bean
    public OneTimeTokenStore samlLoginCodeStore(
            SamlProperties properties, ObjectProvider<StringRedisTemplate> redisTemplate) {
        return new OneTimeTokenStore(
                "saml login code",
                properties.getStateStore(),
                redisTemplate.getIfAvailable(),
                "strata:saml:code:",
                properties.getCodeTtlSeconds());
    }
}
