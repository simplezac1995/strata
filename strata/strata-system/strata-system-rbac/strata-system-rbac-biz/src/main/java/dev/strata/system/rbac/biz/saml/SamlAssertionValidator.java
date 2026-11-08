// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.saml;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Validates an IdP SAMLResponse: secure XML parsing, XML-DSig verification against the IdP metadata certificates,
 * status/issuer/audience/recipient/time/InResponseTo checks, then extracts the email identity.
 *
 * <p>Only the POST binding and the assertion/response signature are supported. The signature must be an enveloped
 * signature over the Response or the Assertion (so adding a second assertion invalidates the digest); exactly one
 * assertion is accepted.
 */
@Service
@ConditionalOnProperty(prefix = "strata.saml", name = "enabled", havingValue = "true")
public class SamlAssertionValidator {

    private static final String SAML_ASSERTION = "urn:oasis:names:tc:SAML:2.0:assertion";
    private static final String SAML_PROTOCOL = "urn:oasis:names:tc:SAML:2.0:protocol";
    private static final XMLSignatureFactory SIGNATURE_FACTORY = XMLSignatureFactory.getInstance("DOM");

    private final SamlProperties properties;
    private final SamlMetadataService metadataService;

    public SamlAssertionValidator(SamlProperties properties, SamlMetadataService metadataService) {
        this.properties = properties;
        this.metadataService = metadataService;
    }

    public SamlIdentity validate(String samlResponse, String expectedRequestId) {
        if (samlResponse == null || samlResponse.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Missing SAMLResponse");
        }
        Document document = parse(decode(samlResponse));
        Element response = document.getDocumentElement();
        if (!"Response".equals(response.getLocalName()) || !SAML_PROTOCOL.equals(response.getNamespaceURI())) {
            throw rejected("Not a SAML Response");
        }
        if (!statusSuccess(response)) {
            throw rejected("SAML authentication was not successful");
        }
        String responseId = attribute(response, "ID");
        NodeList assertions = document.getElementsByTagNameNS(SAML_ASSERTION, "Assertion");
        if (assertions.getLength() != 1) {
            throw rejected("Expected exactly one SAML assertion");
        }
        Element assertion = (Element) assertions.item(0);
        String assertionId = attribute(assertion, "ID");
        if (!signatureValid(document, responseId, assertionId)) {
            throw rejected("SAML signature is missing or invalid");
        }
        validateIssuer(assertion);
        validateConditions(assertion);
        validateSubjectConfirmation(assertion, expectedRequestId);
        return extractIdentity(assertion);
    }

    private static BusinessException rejected(String message) {
        return new BusinessException(ErrorCode.UNAUTHORIZED, message);
    }

    private Document parse(byte[] xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Malformed SAMLResponse");
        }
    }

    private byte[] decode(String samlResponse) {
        try {
            return Base64.getMimeDecoder().decode(samlResponse);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "SAMLResponse is not valid base64");
        }
    }

    private static boolean statusSuccess(Element response) {
        NodeList codes = response.getElementsByTagNameNS(SAML_PROTOCOL, "StatusCode");
        return codes.getLength() > 0
                && "urn:oasis:names:tc:SAML:2.0:status:Success".equals(attribute((Element) codes.item(0), "Value"));
    }

    private boolean signatureValid(Document document, String responseId, String assertionId) {
        NodeList signatures = document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        List<X509Certificate> certificates = metadataService.metadata().certificates();
        for (int index = 0; index < signatures.getLength(); index++) {
            Element signatureElement = (Element) signatures.item(index);
            if (!(signatureElement.getParentNode() instanceof Element parent)) {
                continue;
            }
            String parentId = attribute(parent, "ID");
            if (parentId == null || (!parentId.equals(responseId) && !parentId.equals(assertionId))) {
                continue;
            }
            for (X509Certificate certificate : certificates) {
                try {
                    DOMValidateContext context = new DOMValidateContext(certificate.getPublicKey(), signatureElement);
                    registerIds(document.getDocumentElement(), context);
                    var signature = SIGNATURE_FACTORY.unmarshalXMLSignature(new DOMStructure(signatureElement));
                    if (!signature.validate(context)) {
                        continue;
                    }
                    for (Object reference : signature.getSignedInfo().getReferences()) {
                        String uri = ((javax.xml.crypto.dsig.Reference) reference).getURI();
                        if (("#" + responseId).equals(uri) || ("#" + assertionId).equals(uri)) {
                            return true;
                        }
                    }
                } catch (Exception ignored) {
                    // try the next certificate/algorithm combination
                }
            }
        }
        return false;
    }

    private static void registerIds(Node node, DOMValidateContext context) {
        if (node instanceof Element element) {
            if (element.hasAttribute("ID")) {
                context.setIdAttributeNS(element, null, "ID");
            } else if (element.hasAttribute("Id")) {
                context.setIdAttributeNS(element, null, "Id");
            }
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            registerIds(child, context);
        }
    }

    private void validateIssuer(Element assertion) {
        String issuer = text(assertion, SAML_ASSERTION, "Issuer");
        if (issuer == null || !issuer.equals(metadataService.metadata().entityId())) {
            throw rejected("SAML issuer does not match the configured IdP");
        }
    }

    private void validateConditions(Element assertion) {
        Element conditions = child(assertion, SAML_ASSERTION, "Conditions");
        if (conditions == null) {
            throw rejected("SAML assertion has no Conditions");
        }
        Instant now = Instant.now();
        long skew = properties.getClockSkewSeconds();
        String notBefore = attribute(conditions, "NotBefore");
        if (notBefore != null && parseInstant(notBefore).minusSeconds(skew).isAfter(now)) {
            throw rejected("SAML assertion is not yet valid");
        }
        String notOnOrAfter = attribute(conditions, "NotOnOrAfter");
        if (notOnOrAfter != null && parseInstant(notOnOrAfter).plusSeconds(skew).isBefore(now)) {
            throw rejected("SAML assertion has expired");
        }
        NodeList audiences = conditions.getElementsByTagNameNS(SAML_ASSERTION, "Audience");
        boolean audienceOk = false;
        for (int index = 0; index < audiences.getLength(); index++) {
            if (properties
                    .getSpEntityId()
                    .equals(audiences.item(index).getTextContent().trim())) {
                audienceOk = true;
            }
        }
        if (!audienceOk) {
            throw rejected("SAML audience does not include this service provider");
        }
    }

    private void validateSubjectConfirmation(Element assertion, String expectedRequestId) {
        Element subject = child(assertion, SAML_ASSERTION, "Subject");
        if (subject == null) {
            throw rejected("SAML assertion has no Subject");
        }
        NodeList confirmations = subject.getElementsByTagNameNS(SAML_ASSERTION, "SubjectConfirmationData");
        boolean accepted = false;
        Instant now = Instant.now();
        for (int index = 0; index < confirmations.getLength(); index++) {
            Element data = (Element) confirmations.item(index);
            String recipient = attribute(data, "Recipient");
            String notOnOrAfter = attribute(data, "NotOnOrAfter");
            String inResponseTo = attribute(data, "InResponseTo");
            if (recipient != null && !recipient.equals(properties.getAcsUrl())) {
                continue;
            }
            if (notOnOrAfter != null
                    && parseInstant(notOnOrAfter)
                            .plusSeconds(properties.getClockSkewSeconds())
                            .isBefore(now)) {
                continue;
            }
            if (expectedRequestId != null && inResponseTo != null && !inResponseTo.equals(expectedRequestId)) {
                continue;
            }
            accepted = true;
            break;
        }
        if (!accepted) {
            throw rejected("SAML SubjectConfirmationData is not valid for this service provider");
        }
    }

    private SamlIdentity extractIdentity(Element assertion) {
        String subject = firstNonBlank(
                text(assertion, SAML_ASSERTION, "NameID"),
                attribute(child(assertion, SAML_ASSERTION, "Subject") == null ? null : assertion, "Subject"));
        Element originalSubject = child(assertion, SAML_ASSERTION, "Subject");
        if (originalSubject != null) {
            String nameId = text(originalSubject, SAML_ASSERTION, "NameID");
            if (nameId != null && !nameId.isBlank()) {
                subject = nameId.trim();
            }
        }
        Map<String, List<String>> attributes = attributes(assertion);
        String email = subject != null && subject.contains("@") ? subject : null;
        List<String> candidates = List.of(
                "email",
                "mail",
                "urn:oid:1.2.840.113549.1.9.1",
                "urn:oid:0.9.2342.19200300.100.1.3",
                "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress");
        for (String candidate : candidates) {
            if (email != null) {
                break;
            }
            for (String value : attributes.getOrDefault(candidate, List.of())) {
                if (value.contains("@")) {
                    email = value;
                    break;
                }
            }
        }
        if (email == null || email.isBlank()) {
            throw rejected("SAML assertion has no email identity");
        }
        String displayName = firstAttribute(attributes, "displayName", "name", "cn");
        if (displayName == null || displayName.isBlank()) {
            displayName = email.substring(0, email.indexOf('@'));
        }
        return new SamlIdentity(subject == null ? email : subject, email.trim().toLowerCase(), displayName.trim());
    }

    private static Map<String, List<String>> attributes(Element assertion) {
        Map<String, List<String>> result = new HashMap<>();
        NodeList attributeNodes = assertion.getElementsByTagNameNS(SAML_ASSERTION, "Attribute");
        for (int index = 0; index < attributeNodes.getLength(); index++) {
            Element attribute = (Element) attributeNodes.item(index);
            String name = attribute.getAttribute("Name");
            List<String> values = new ArrayList<>();
            NodeList valueNodes = attribute.getElementsByTagNameNS(SAML_ASSERTION, "AttributeValue");
            for (int valueIndex = 0; valueIndex < valueNodes.getLength(); valueIndex++) {
                values.add(valueNodes.item(valueIndex).getTextContent().trim());
            }
            result.put(name, values);
        }
        return result;
    }

    private static String firstAttribute(Map<String, List<String>> attributes, String... names) {
        for (String name : names) {
            List<String> values = attributes.get(name);
            if (values != null && !values.isEmpty() && !values.get(0).isBlank()) {
                return values.get(0);
            }
        }
        return null;
    }

    private static String childText(Element element, String namespace, String name) {
        return text(element, namespace, name);
    }

    private static Element child(Element element, String namespace, String name) {
        NodeList nodes = element.getElementsByTagNameNS(namespace, name);
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    private static String text(Element element, String namespace, String name) {
        NodeList nodes = element.getElementsByTagNameNS(namespace, name);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent().trim();
    }

    private static String attribute(Element element, String name) {
        return element == null ? null : (element.hasAttribute(name) ? element.getAttribute(name) : null);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            return OffsetDateTime.parse(value).toInstant();
        }
    }
}
