// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.seed;

import dev.strata.system.api.seed.SystemSeedFile;
import dev.strata.system.communication.api.entity.Announcement;
import dev.strata.system.communication.api.entity.Webhook;
import dev.strata.system.communication.biz.repository.AnnouncementRepository;
import dev.strata.system.communication.biz.repository.WebhookRepository;
import dev.strata.system.config.api.entity.EmailTemplate;
import dev.strata.system.config.api.entity.Region;
import dev.strata.system.config.api.entity.SensitiveWord;
import dev.strata.system.config.biz.repository.EmailTemplateRepository;
import dev.strata.system.config.biz.repository.RegionRepository;
import dev.strata.system.config.biz.repository.SensitiveWordRepository;
import dev.strata.system.rbac.api.entity.OauthClient;
import dev.strata.system.rbac.api.entity.Tenant;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.biz.repository.OauthClientRepository;
import dev.strata.system.rbac.biz.repository.TenantRepository;
import dev.strata.system.rbac.biz.repository.UserRepository;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Seeds P4 system-domain demo rows (tenant, oauth, regions, etc.). */
@Component
@Order(2)
@ConditionalOnProperty(name = "strata.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class SystemReferenceDataInitializer implements ApplicationRunner {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final OauthClientRepository oauthClientRepository;
    private final SensitiveWordRepository sensitiveWordRepository;
    private final RegionRepository regionRepository;
    private final WebhookRepository webhookRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final AnnouncementRepository announcementRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    public SystemReferenceDataInitializer(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            OauthClientRepository oauthClientRepository,
            SensitiveWordRepository sensitiveWordRepository,
            RegionRepository regionRepository,
            WebhookRepository webhookRepository,
            EmailTemplateRepository emailTemplateRepository,
            AnnouncementRepository announcementRepository,
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.oauthClientRepository = oauthClientRepository;
        this.sensitiveWordRepository = sensitiveWordRepository;
        this.regionRepository = regionRepository;
        this.webhookRepository = webhookRepository;
        this.emailTemplateRepository = emailTemplateRepository;
        this.announcementRepository = announcementRepository;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SystemSeedFile seed = loadSeed();
        seedTenants(seed);
        seedOauthClients(seed);
        seedSensitiveWords(seed);
        seedRegions(seed);
        seedWebhooks(seed);
        seedEmailTemplates(seed);
        seedAnnouncements(seed);
    }

    private SystemSeedFile loadSeed() {
        try (var in = resourceLoader
                .getResource("classpath:seed/system-seed-data.json")
                .getInputStream()) {
            return objectMapper.readValue(in, SystemSeedFile.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load P4 system seed data", ex);
        }
    }

    private void seedTenants(SystemSeedFile seed) {
        Map<String, Tenant> tenants = new HashMap<>();
        for (Tenant tenant : tenantRepository.findAll()) {
            tenants.put(tenant.getCode(), tenant);
        }
        for (SystemSeedFile.TenantSeed row : seed.tenants()) {
            if (tenants.containsKey(row.code())) {
                continue;
            }
            Tenant tenant = new Tenant();
            tenant.setCode(row.code());
            tenant.setName(row.name());
            tenant.setDomain(row.domain());
            tenant.setStatus(row.status());
            tenant.setPlan(row.plan());
            tenants.put(row.code(), tenantRepository.save(tenant));
        }
        Tenant tenant = tenants.get("default");
        if (tenant != null) {
            backfillUserTenantIds(tenant.getId());
        }
    }

    private void backfillUserTenantIds(Long tenantId) {
        for (User user : userRepository.findAll()) {
            if (user.getTenantId() == null) {
                user.setTenantId(tenantId);
                userRepository.save(user);
            }
        }
    }

    private void seedOauthClients(SystemSeedFile seed) {
        for (SystemSeedFile.OauthClientSeed row : seed.oauthClients()) {
            if (oauthClientRepository.existsByClientId(row.clientId())) {
                continue;
            }
            OauthClient client = new OauthClient();
            client.setClientId(row.clientId());
            client.setName(row.name());
            client.setClientSecret(DigestUtils.sha256Hex(row.secretPlaintext()));
            client.setScopes(row.scopes());
            client.setEnabled(true);
            oauthClientRepository.save(client);
        }
    }

    private void seedSensitiveWords(SystemSeedFile seed) {
        for (SystemSeedFile.SensitiveWordSeed row : seed.sensitiveWords()) {
            if (sensitiveWordRepository.existsByWord(row.word())) {
                continue;
            }
            SensitiveWord word = new SensitiveWord();
            word.setWord(row.word());
            word.setEnabled(true);
            sensitiveWordRepository.save(word);
        }
    }

    private void seedRegions(SystemSeedFile seed) {
        Map<String, Long> regionIds = new HashMap<>();
        for (Region region : regionRepository.findAll()) {
            regionIds.put(region.getCode(), region.getId());
        }
        for (SystemSeedFile.RegionSeed row : seed.regions()) {
            if (regionIds.containsKey(row.code())) {
                continue;
            }
            if (row.parentCode() != null && !regionIds.containsKey(row.parentCode())) {
                throw new IllegalStateException("Missing parent region in seed: " + row.parentCode());
            }
            Region region = new Region();
            region.setCode(row.code());
            region.setName(row.name());
            region.setLevel(row.level());
            if (row.parentCode() != null) {
                region.setParentId(regionIds.get(row.parentCode()));
            }
            regionRepository.save(region);
            regionIds.put(row.code(), region.getId());
        }
    }

    private void seedWebhooks(SystemSeedFile seed) {
        Map<String, Webhook> webhooks = new HashMap<>();
        for (Webhook webhook : webhookRepository.findAll()) {
            webhooks.put(webhook.getUrl(), webhook);
        }
        for (SystemSeedFile.WebhookSeed row : seed.webhooks()) {
            if (webhooks.containsKey(row.url())) {
                continue;
            }
            Webhook webhook = new Webhook();
            webhook.setName(row.name());
            webhook.setUrl(row.url());
            webhook.setSecret(DigestUtils.sha256Hex(row.secretPlaintext()));
            webhook.setEnabled(true);
            webhookRepository.save(webhook);
        }
    }

    private void seedEmailTemplates(SystemSeedFile seed) {
        for (SystemSeedFile.EmailTemplateSeed row : seed.emailTemplates()) {
            if (emailTemplateRepository.existsByCode(row.code())) {
                continue;
            }
            EmailTemplate template = new EmailTemplate();
            template.setCode(row.code());
            template.setName(row.name());
            template.setSubject(row.subject());
            template.setBody(row.body());
            template.setEnabled(true);
            emailTemplateRepository.save(template);
        }
    }

    private void seedAnnouncements(SystemSeedFile seed) {
        Map<String, Announcement> announcements = new HashMap<>();
        for (Announcement announcement : announcementRepository.findAll()) {
            announcements.put(announcement.getTitle(), announcement);
        }
        for (SystemSeedFile.AnnouncementSeed row : seed.announcements()) {
            if (announcements.containsKey(row.title())) {
                continue;
            }
            Announcement announcement = new Announcement();
            announcement.setTitle(row.title());
            announcement.setContent(row.content());
            announcement.setEnabled(true);
            announcementRepository.save(announcement);
        }
    }
}
