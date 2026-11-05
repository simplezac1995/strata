// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.system.api.dto.AnnouncementDto;
import dev.strata.system.api.dto.WebhookDto;
import dev.strata.system.api.request.AnnouncementCreateRequest;
import dev.strata.system.api.request.AnnouncementUpdateRequest;
import dev.strata.system.api.request.WebhookCreateRequest;
import dev.strata.system.api.request.WebhookUpdateRequest;
import dev.strata.system.communication.api.entity.Announcement;
import dev.strata.system.communication.api.entity.Webhook;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Communication 模块 Mapper 接口契约单元测试：覆盖 null-check 与 defaultValue 分支，不启动 Spring 容器。
 */
class CommunicationMapperTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final WebhookMapper webhookMapper = new WebhookMapperImpl();
    private final AnnouncementMapper announcementMapper = new AnnouncementMapperImpl();

    @Nested
    @DisplayName("WebhookMapper")
    class WebhookMapperCases {

        @Test
        @DisplayName("toEntity(null) 返回 null")
        void toEntityNull() {
            assertThat(webhookMapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("toEntity enabled 显式非空时使用请求值")
        void toEntityEnabledExplicit() {
            Webhook webhook = webhookMapper.toEntity(
                    new WebhookCreateRequest("hook", "https://example.com/hook", "secret", false));
            assertThat(webhook.getName()).isEqualTo("hook");
            assertThat(webhook.getUrl()).isEqualTo("https://example.com/hook");
            assertThat(webhook.getSecret()).isEqualTo("secret");
            assertThat(webhook.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("toEntity enabled 为 null 时默认 true（defaultValue 分支）")
        void toEntityEnabledDefault() {
            Webhook webhook =
                    webhookMapper.toEntity(new WebhookCreateRequest("hook", "https://example.com/hook", null, null));
            assertThat(webhook.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("toDto(null) 返回 null；全字段与 null id 分支；secret 始终忽略")
        void toDtoBranches() {
            assertThat(webhookMapper.toDto(null)).isNull();

            Webhook webhook = new Webhook();
            webhook.setId(8L);
            webhook.setName("hook");
            webhook.setUrl("https://example.com/hook");
            webhook.setEnabled(true);
            WebhookDto dto = webhookMapper.toDto(webhook);
            assertThat(dto.id()).isEqualTo("8");
            assertThat(dto.name()).isEqualTo("hook");
            assertThat(dto.url()).isEqualTo("https://example.com/hook");
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.secret()).isNull();

            webhook.setId(null);
            assertThat(webhookMapper.toDto(webhook).id()).isNull();
        }

        @Test
        @DisplayName("update(null, webhook) 不改；可空字段 null 时保持原值")
        void updateBranches() {
            Webhook webhook = new Webhook();
            webhook.setName("old");
            webhook.setUrl("https://old.example.com");
            webhook.setEnabled(true);

            webhookMapper.update(null, webhook);
            assertThat(webhook.getName()).isEqualTo("old");

            webhookMapper.update(new WebhookUpdateRequest("new", "https://new.example.com", null, false), webhook);
            assertThat(webhook.getName()).isEqualTo("new");
            assertThat(webhook.getUrl()).isEqualTo("https://new.example.com");
            assertThat(webhook.isEnabled()).isFalse();

            webhookMapper.update(new WebhookUpdateRequest(null, null, null, null), webhook);
            assertThat(webhook.getName()).isEqualTo("new");
            assertThat(webhook.getUrl()).isEqualTo("https://new.example.com");
            assertThat(webhook.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("AnnouncementMapper")
    class AnnouncementMapperCases {

        @Test
        @DisplayName("toEntity(null) 返回 null")
        void toEntityNull() {
            assertThat(announcementMapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("toEntity enabled 显式非空时使用请求值")
        void toEntityEnabledExplicit() {
            Announcement announcement = announcementMapper.toEntity(new AnnouncementCreateRequest("标题", "内容", false));
            assertThat(announcement.getTitle()).isEqualTo("标题");
            assertThat(announcement.getContent()).isEqualTo("内容");
            assertThat(announcement.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("toEntity enabled 为 null 时默认 true（defaultValue 分支）")
        void toEntityEnabledDefault() {
            Announcement announcement = announcementMapper.toEntity(new AnnouncementCreateRequest("标题", "内容", null));
            assertThat(announcement.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("toDto(null) 返回 null；全字段与 null id 分支")
        void toDtoBranches() {
            assertThat(announcementMapper.toDto(null)).isNull();

            Announcement announcement = new Announcement();
            announcement.setId(6L);
            announcement.setTitle("标题");
            announcement.setContent("内容");
            announcement.setEnabled(true);
            announcement.setCreatedAt(NOW);
            announcement.setUpdatedAt(NOW.plusSeconds(10));
            AnnouncementDto dto = announcementMapper.toDto(announcement);
            assertThat(dto.id()).isEqualTo("6");
            assertThat(dto.title()).isEqualTo("标题");
            assertThat(dto.content()).isEqualTo("内容");
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.createdAt()).isEqualTo(NOW);
            assertThat(dto.updatedAt()).isEqualTo(NOW.plusSeconds(10));

            announcement.setId(null);
            assertThat(announcementMapper.toDto(announcement).id()).isNull();
        }

        @Test
        @DisplayName("update(null, 公告) 不改；可空字段 null 时保持原值")
        void updateBranches() {
            Announcement announcement = new Announcement();
            announcement.setTitle("旧标题");
            announcement.setContent("旧内容");
            announcement.setEnabled(true);

            announcementMapper.update(null, announcement);
            assertThat(announcement.getTitle()).isEqualTo("旧标题");

            announcementMapper.update(new AnnouncementUpdateRequest("新标题", "新内容", false), announcement);
            assertThat(announcement.getTitle()).isEqualTo("新标题");
            assertThat(announcement.getContent()).isEqualTo("新内容");
            assertThat(announcement.isEnabled()).isFalse();

            announcementMapper.update(new AnnouncementUpdateRequest(null, null, null), announcement);
            assertThat(announcement.getTitle()).isEqualTo("新标题");
            assertThat(announcement.getContent()).isEqualTo("新内容");
            assertThat(announcement.isEnabled()).isFalse();
        }
    }
}
