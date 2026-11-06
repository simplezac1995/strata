// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

/**
 * Response from the social-login authorize endpoint (P2-9 skeleton). The {@code state} echoes back to the
 * browser and must be returned verbatim on the callback; the PKCE {@code codeChallenge} is the public
 * challenge sent to the provider. The matching {@code codeVerifier} never leaves the server.
 */
public record SocialAuthorizeResponse(String state, String authorizeUrl, String codeChallenge) {}
