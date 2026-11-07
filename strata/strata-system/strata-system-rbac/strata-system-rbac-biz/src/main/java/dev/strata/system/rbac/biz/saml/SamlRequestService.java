// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.saml;

import dev.strata.system.rbac.biz.saml.support.OneTimeTokenStore;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Builds SP-initiated AuthnRequests (HTTP-Redirect binding) and keeps the request id bound to the RelayState. */
@Service
@ConditionalOnProperty(prefix = "strata.saml", name = "enabled", havingValue = "true")
public class SamlRequestService {

    private final SamlProperties properties;
    private final SamlMetadataService metadataService;
    private final OneTimeTokenStore relayStateStore;

    public SamlRequestService(
            SamlProperties properties, SamlMetadataService metadataService, OneTimeTokenStore samlRelayStateStore) {
        this.properties = properties;
        this.metadataService = metadataService;
        this.relayStateStore = samlRelayStateStore;
    }

    /** Returns the URL to redirect the browser to (IdP SSO endpoint with SAMLRequest + RelayState). */
    public String beginUrl() {
        SamlMetadataService.IdpMetadata metadata = metadataService.metadata();
        String requestId = "_" + UUID.randomUUID();
        String relayState = relayStateStore.newKey();
        relayStateStore.put(relayState, requestId);
        String xml = "<samlp:AuthnRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\""
                + " xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\""
                + " ID=\"" + requestId + "\" Version=\"2.0\""
                + " IssueInstant=\"" + Instant.now() + "\""
                + " Destination=\"" + metadata.ssoUrl() + "\""
                + " AssertionConsumerServiceURL=\"" + properties.getAcsUrl() + "\""
                + " ProtocolBinding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\">"
                + "<saml:Issuer>" + properties.getSpEntityId() + "</saml:Issuer>"
                + "<samlp:NameIDPolicy Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\""
                + " AllowCreate=\"true\"/>"
                + "</samlp:AuthnRequest>";
        String encoded = URLEncoder.encode(deflateBase64(xml), StandardCharsets.UTF_8);
        return metadata.ssoUrl()
                + (metadata.ssoUrl().contains("?") ? "&" : "?")
                + "SAMLRequest=" + encoded
                + "&RelayState=" + URLEncoder.encode(relayState, StandardCharsets.UTF_8);
    }

    /** Consumes the RelayState exactly once and returns the AuthnRequest id it belongs to. */
    public String requestIdFor(String relayState) {
        return relayStateStore.take(relayState);
    }

    private static String deflateBase64(String xml) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
            try (DeflaterOutputStream stream = new DeflaterOutputStream(buffer, deflater)) {
                stream.write(xml.getBytes(StandardCharsets.UTF_8));
            } finally {
                deflater.end();
            }
            return Base64.getEncoder().encodeToString(buffer.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("could not build the SAML AuthnRequest", exception);
        }
    }
}
