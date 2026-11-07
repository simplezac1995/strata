// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.entity;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.system.rbac.api.entity.ApiKey;
import dev.strata.system.rbac.api.entity.OauthClient;
import dev.strata.system.rbac.api.entity.User;
import org.junit.jupiter.api.Test;

/**
 * P1-8 credential hygiene: Lombok-generated {@code toString()} on credential-bearing entities must never
 * emit the password hash, reset token, TOTP secret, OAuth client secret, or API key hash, so a logged entity
 * dump cannot leak them.
 */
class CredentialEntityToStringTest {

    @Test
    void userToStringHidesCredentials() {
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("SUPER_SECRET_PASSWORD_HASH");
        user.setResetToken("SUPER_SECRET_RESET_TOKEN");
        user.setTotpSecret("SUPER_SECRET_TOTP");

        String out = user.toString();

        assertThat(out).contains("alice");
        assertThat(out).doesNotContain("SUPER_SECRET_PASSWORD_HASH");
        assertThat(out).doesNotContain("passwordHash=");
        assertThat(out).doesNotContain("SUPER_SECRET_RESET_TOKEN");
        // resetTokenExpiry is a separate, non-sensitive field and may appear; resetToken itself must not.
        assertThat(out).doesNotContain("resetToken=");
        assertThat(out).doesNotContain("SUPER_SECRET_TOTP");
        assertThat(out).doesNotContain("totpSecret=");
    }

    @Test
    void oauthClientToStringHidesSecret() {
        OauthClient client = new OauthClient();
        client.setClientId("cid");
        client.setClientSecret("SUPER_SECRET_CLIENT_SECRET");

        String out = client.toString();

        assertThat(out).contains("cid");
        assertThat(out).doesNotContain("SUPER_SECRET_CLIENT_SECRET");
        assertThat(out).doesNotContain("clientSecret");
    }

    @Test
    void apiKeyToStringHidesHash() {
        ApiKey key = new ApiKey();
        key.setName("ops");
        key.setKeyHash("SUPER_SECRET_KEY_HASH");

        String out = key.toString();

        assertThat(out).contains("ops");
        assertThat(out).doesNotContain("SUPER_SECRET_KEY_HASH");
        assertThat(out).doesNotContain("keyHash");
    }
}
