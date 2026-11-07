// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.config;

import dev.strata.common.core.spi.VerificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default {@link VerificationSender}: logs that a code was requested without shipping a transport. Deployments
 * override it with their own bean (SMTP, SMS gateway, notification service).
 */
@Configuration(proxyBeanMethods = false)
public class VerificationConfiguration {

    private static final Logger log = LoggerFactory.getLogger(VerificationConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(VerificationSender.class)
    VerificationSender loggingVerificationSender() {
        return new VerificationSender() {
            @Override
            public void sendEmailVerificationCode(String email, String code) {
                log.info("email verification code requested for {} (no VerificationSender configured)", mask(email));
            }

            @Override
            public void sendSmsVerificationCode(String phone, String code) {
                log.info("SMS verification code requested for {} (no VerificationSender configured)", mask(phone));
            }

            private String mask(String value) {
                if (value == null || value.length() <= 4) return "***";
                return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
            }
        };
    }
}
