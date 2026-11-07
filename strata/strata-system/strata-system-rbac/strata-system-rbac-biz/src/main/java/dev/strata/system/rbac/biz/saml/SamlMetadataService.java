// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.saml;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Loads the IdP's SAML metadata (entity id, SSO URL, signing certificate) once at startup. The metadata URI points at
 * the IdP's internal address; the SSO URL inside the metadata is the browser-facing one, which is what we redirect to.
 */
@Service
@ConditionalOnProperty(prefix = "strata.saml", name = "enabled", havingValue = "true")
public class SamlMetadataService {

    private static final Logger log = LoggerFactory.getLogger(SamlMetadataService.class);
    private static final String MD_NS = "urn:oasis:names:tc:SAML:2.0:metadata";
    private static final String DS_NS = "http://www.w3.org/2000/09/xmldsig#";

    private final SamlProperties properties;
    private volatile IdpMetadata metadata;

    public SamlMetadataService(SamlProperties properties) {
        this.properties = properties;
    }

    public IdpMetadata metadata() {
        IdpMetadata loaded = metadata;
        if (loaded == null) {
            synchronized (this) {
                loaded = metadata;
                if (loaded == null) {
                    metadata = loaded = load();
                }
            }
        }
        return loaded;
    }

    /** Eager warm-up so a misconfigured SAML setup fails at startup instead of at the first login. */
    @jakarta.annotation.PostConstruct
    public void warmUp() {
        requireConfigured(properties.getSpEntityId(), "strata.saml.sp-entity-id");
        requireConfigured(properties.getAcsUrl(), "strata.saml.acs-url");
        requireConfigured(properties.getFrontendCallbackUrl(), "strata.saml.frontend-callback-url");
        metadata();
    }

    private static void requireConfigured(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("SAML is enabled but " + name + " is not set");
        }
    }

    private IdpMetadata load() {
        if (properties.getIdpMetadataUri() != null
                && !properties.getIdpMetadataUri().isBlank()) {
            RuntimeException last = null;
            for (int attempt = 1; attempt <= 20; attempt++) {
                try {
                    return fromMetadata(fetch(properties.getIdpMetadataUri().trim()));
                } catch (RuntimeException exception) {
                    last = exception;
                    log.warn("SAML metadata not ready (attempt {}): {}", attempt, exception.getMessage());
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            throw new IllegalStateException("Could not load SAML IdP metadata", last);
        }
        if (properties.getIdpEntityId() == null
                || properties.getIdpSsoUrl() == null
                || properties.getIdpCertificates().isEmpty()) {
            throw new IllegalStateException(
                    "SAML: set either strata.saml.idp-metadata-uri or idp-entity-id + idp-sso-url + idp-certificates");
        }
        return new IdpMetadata(
                properties.getIdpEntityId(),
                properties.getIdpSsoUrl(),
                properties.getIdpCertificates().stream().map(this::certificate).toList());
    }

    private String fetch(String uri) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("metadata HTTP " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("metadata fetch interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("metadata fetch failed: " + exception.getMessage(), exception);
        }
    }

    private IdpMetadata fromMetadata(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setExpandEntityReferences(false);
            Document document =
                    factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Element root = document.getDocumentElement();
            String entityId = root.getAttribute("entityID");
            String ssoUrl = null;
            NodeList ssoNodes = document.getElementsByTagNameNS(MD_NS, "SingleSignOnService");
            for (int index = 0; index < ssoNodes.getLength(); index++) {
                Element element = (Element) ssoNodes.item(index);
                String binding = element.getAttribute("Binding");
                if (ssoUrl == null || binding.endsWith("HTTP-Redirect")) {
                    ssoUrl = element.getAttribute("Location");
                }
                if (binding.endsWith("HTTP-Redirect")) {
                    break;
                }
            }
            List<X509Certificate> certificates = new ArrayList<>();
            NodeList certNodes = document.getElementsByTagNameNS(DS_NS, "X509Certificate");
            for (int index = 0; index < certNodes.getLength(); index++) {
                certificates.add(certificate(certNodes.item(index).getTextContent()));
            }
            if (entityId.isBlank() || ssoUrl == null || ssoUrl.isBlank() || certificates.isEmpty()) {
                throw new IllegalStateException("metadata is missing entityID, SSO location or certificate");
            }
            return new IdpMetadata(entityId, ssoUrl, List.copyOf(certificates));
        } catch (Exception exception) {
            throw new IllegalStateException("invalid SAML metadata: " + exception.getMessage(), exception);
        }
    }

    private X509Certificate certificate(String base64Der) {
        try {
            byte[] der = Base64.getMimeDecoder().decode(stripPem(base64Der));
            return (X509Certificate)
                    CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(der));
        } catch (Exception exception) {
            throw new IllegalStateException("invalid IdP certificate", exception);
        }
    }

    private static String stripPem(String value) {
        return value.replaceAll("-----BEGIN CERTIFICATE-----", "")
                .replaceAll("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
    }

    /** IdP material needed by the SP. */
    public record IdpMetadata(String entityId, String ssoUrl, List<X509Certificate> certificates) {}
}
