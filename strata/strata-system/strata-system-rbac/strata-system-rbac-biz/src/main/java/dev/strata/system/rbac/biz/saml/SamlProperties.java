// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.saml;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SAML 2.0 service-provider settings ({@code strata.saml.*}). Disabled by default.
 *
 * <p>The SP is a deliberate, small implementation: HTTP-Redirect AuthnRequest + HTTP-POST Response (the profile every
 * IdP supports), signature verification with the JDK XML-DSig APIs, no XML external entities, and the same one-time-code
 * hand-off to the SPA that the OIDC flow uses. SAML assertions are mapped to local users by email.
 */
@ConfigurationProperties(prefix = "strata.saml")
public class SamlProperties {

    private boolean enabled = false;
    /** Browser-facing label for the login button. */
    private String displayName = "SAML";
    /** IdP metadata XML URL; alternative to setting the explicit values below. */
    private String idpMetadataUri;
    /** IdP entity id, required when metadata is not used. */
    private String idpEntityId;
    /** IdP SSO endpoint (browser-facing), required when metadata is not used. */
    private String idpSsoUrl;
    /** Base64 DER certificate(s) of the IdP signing key, required when metadata is not used. */
    private List<String> idpCertificates = List.of();
    /** SP entity id sent in AuthnRequests and expected in the Audience restriction. */
    private String spEntityId;
    /** Browser-facing ACS URL registered at the IdP. */
    private String acsUrl;
    /** Where the backend sends the browser after a successful ACS, before the SPA exchanges the code. */
    private String frontendCallbackUrl;
    /** Role granted to auto-provisioned users (empty = no role; an admin assigns one). */
    private String defaultRoleCode = "";

    private boolean autoProvision = true;
    /** One-time state store: {@code memory} (single instance) or {@code redis}. */
    private String stateStore = "memory";
    /** Lifetime of the one-time login code handed to the SPA. */
    private int codeTtlSeconds = 60;
    /** Allowed clock skew for assertion conditions. */
    private int clockSkewSeconds = 120;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDisplayName() {
        return displayName == null || displayName.isBlank() ? "SAML" : displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getIdpMetadataUri() {
        return idpMetadataUri;
    }

    public void setIdpMetadataUri(String idpMetadataUri) {
        this.idpMetadataUri = idpMetadataUri;
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }

    public void setIdpEntityId(String idpEntityId) {
        this.idpEntityId = idpEntityId;
    }

    public String getIdpSsoUrl() {
        return idpSsoUrl;
    }

    public void setIdpSsoUrl(String idpSsoUrl) {
        this.idpSsoUrl = idpSsoUrl;
    }

    public List<String> getIdpCertificates() {
        return idpCertificates == null ? List.of() : idpCertificates;
    }

    public void setIdpCertificates(List<String> idpCertificates) {
        this.idpCertificates = idpCertificates;
    }

    public String getSpEntityId() {
        return spEntityId;
    }

    public void setSpEntityId(String spEntityId) {
        this.spEntityId = spEntityId;
    }

    public String getAcsUrl() {
        return acsUrl;
    }

    public void setAcsUrl(String acsUrl) {
        this.acsUrl = acsUrl;
    }

    public String getFrontendCallbackUrl() {
        return frontendCallbackUrl;
    }

    public void setFrontendCallbackUrl(String frontendCallbackUrl) {
        this.frontendCallbackUrl = frontendCallbackUrl;
    }

    public String getDefaultRoleCode() {
        return defaultRoleCode == null ? "" : defaultRoleCode;
    }

    public void setDefaultRoleCode(String defaultRoleCode) {
        this.defaultRoleCode = defaultRoleCode;
    }

    public boolean isAutoProvision() {
        return autoProvision;
    }

    public void setAutoProvision(boolean autoProvision) {
        this.autoProvision = autoProvision;
    }

    public String getStateStore() {
        return stateStore == null ? "memory" : stateStore;
    }

    public void setStateStore(String stateStore) {
        this.stateStore = stateStore;
    }

    public int getCodeTtlSeconds() {
        return codeTtlSeconds <= 0 ? 60 : codeTtlSeconds;
    }

    public void setCodeTtlSeconds(int codeTtlSeconds) {
        this.codeTtlSeconds = codeTtlSeconds;
    }

    public int getClockSkewSeconds() {
        return clockSkewSeconds < 0 ? 0 : clockSkewSeconds;
    }

    public void setClockSkewSeconds(int clockSkewSeconds) {
        this.clockSkewSeconds = clockSkewSeconds;
    }
}
