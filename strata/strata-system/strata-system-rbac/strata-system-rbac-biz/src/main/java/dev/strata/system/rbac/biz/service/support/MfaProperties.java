// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * TOTP MFA settings.
 *
 * <p>{@code issuer} is embedded in the `otpauth://` URI as well as the QR code, so it is the label users see in their
 * authenticator app. Keep it stable per deployment: changing it relabels new enrollments, not existing secrets.
 */
@Component
@ConfigurationProperties(prefix = "strata.mfa")
public class MfaProperties {

    /** Issuer label shown by authenticator apps (default: "Strata Admin"). */
    private String issuer = "Strata Admin";

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
