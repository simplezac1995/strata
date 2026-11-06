// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.seed;

import java.util.List;

public record SystemSeedFile(
        List<TenantSeed> tenants,
        List<OauthClientSeed> oauthClients,
        List<SensitiveWordSeed> sensitiveWords,
        List<RegionSeed> regions,
        List<WebhookSeed> webhooks,
        List<EmailTemplateSeed> emailTemplates,
        List<AnnouncementSeed> announcements) {

    public record TenantSeed(String code, String name, String domain, String status, String plan) {}

    public record OauthClientSeed(String clientId, String name, String secretPlaintext, String scopes) {}

    public record SensitiveWordSeed(String word) {}

    public record RegionSeed(String code, String name, int level, String parentCode) {}

    public record WebhookSeed(String name, String url, String secretPlaintext) {}

    public record EmailTemplateSeed(String code, String name, String subject, String body) {}

    public record AnnouncementSeed(String title, String content) {}
}
