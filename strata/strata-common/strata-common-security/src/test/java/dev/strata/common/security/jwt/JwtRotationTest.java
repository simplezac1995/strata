// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Key rotation, which is the difference between a secret that can be changed and one that never is.
 *
 * <p>The failure this prevents is mundane and common: rotate the secret, invalidate every session at once, decide
 * never to rotate again.
 */
@SuppressWarnings({"unchecked", "varargs"})
class JwtRotationTest {

    private static final String OLD_SECRET = "old-secret-at-least-32-characters-long";
    private static final String NEW_SECRET = "new-secret-at-least-32-characters-long";
    private static final String OTHER_SECRET = "unrelated-secret-32-characters-long!!";

    private static JwtTokenService service(String secret, String previous) {
        return new JwtTokenService(new JwtProperties(secret, previous, 7200, 604800));
    }

    @Test
    void verifies_its_own_tokens() {
        JwtTokenService tokens = service(NEW_SECRET, null);

        Claims claims = tokens.parse(tokens.createAccessToken(7L, "admin", List.of("ROLE_ADMIN")));

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(tokens.isAccessToken(claims)).isTrue();
        assertThat(claims.get("authorities", List.class)).containsExactly("ROLE_ADMIN");
    }

    @Test
    void a_token_issued_before_a_rotation_still_verifies() {
        // What an operator does: issue tokens under the old key, then deploy with the new one as current and the
        // old one as previous. Nobody is logged out, and the old key retires on its own as tokens expire.
        String issuedBefore = service(OLD_SECRET, null).createAccessToken(42L, "operator", List.of("ROLE_STAFF"));

        Claims verified = service(NEW_SECRET, OLD_SECRET).parse(issuedBefore);

        assertThat(verified.getSubject()).isEqualTo("42");
    }

    @Test
    void the_old_key_stops_being_accepted_once_the_rotation_is_finished() {
        String issuedBefore = service(OLD_SECRET, null).createAccessToken(42L, "operator", List.of("ROLE_STAFF"));

        assertThatThrownBy(() -> service(NEW_SECRET, null).parse(issuedBefore))
                .as("clearing the previous secret is what ends the overlap, and it must actually end it")
                .isInstanceOf(JwtException.class);
    }

    @Test
    void a_token_signed_by_a_key_we_do_not_hold_is_rejected() {
        String foreign = service(OTHER_SECRET, null).createAccessToken(1L, "attacker", List.of("ROLE_ADMIN"));

        assertThatThrownBy(() -> service(NEW_SECRET, OLD_SECRET).parse(foreign))
                .as("the signature decides, not the key id in the header")
                .isInstanceOf(JwtException.class);
    }

    @Test
    void the_fingerprint_identifies_the_key_without_exposing_it() {
        JwtTokenService tokens = service(NEW_SECRET, null);

        assertThat(tokens.fingerprint())
                .as("short enough to compare two deployments by eye in a log line")
                .hasSize(8)
                .isNotEqualTo(NEW_SECRET)
                .doesNotContain(NEW_SECRET.substring(0, 8));
        assertThat(service(OLD_SECRET, null).fingerprint())
                .as("two different keys must not look the same")
                .isNotEqualTo(tokens.fingerprint());
    }

    @Test
    void a_short_previous_secret_is_refused_rather_than_ignored() {
        assertThatThrownBy(() -> new JwtProperties(NEW_SECRET, "too-short", 7200, 604800))
                .as("ignoring it silently would log everyone out at the moment the operator believed otherwise")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("previous-secret");
    }

    @Test
    void a_blank_previous_secret_means_no_rotation_is_configured() {
        assertThat(new JwtProperties(NEW_SECRET, "  ", 7200, 604800).hasPreviousSecret())
                .isFalse();
    }
}
