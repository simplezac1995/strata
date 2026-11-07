// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.saml;

/** Identity extracted from a validated SAML assertion. */
public record SamlIdentity(String subject, String email, String displayName) {}
