// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.biz.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.ai.api.request.AiChatAttachment;
import dev.strata.ai.chat.api.entity.AiChatMessage;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.plugin.api.llm.LlmChatCommand;
import dev.strata.plugin.api.llm.LlmContentPart;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LlmGatewaySupport 提示词组装")
class LlmGatewaySupportTest {

    @Test
    @DisplayName("把 provider/model/历史/新消息组装成 OpenAI 兼容命令")
    void toCommand_assembles_credentials_history_and_user_message() {
        AiProvider provider = new AiProvider();
        provider.setBaseUrl("https://llm.example.com");
        provider.setApiKey("sk-secret");
        AiModel model = new AiModel();
        model.setCode("gpt-test");

        AiChatMessage h1 = new AiChatMessage();
        h1.setRole("user");
        h1.setContent("hi");
        AiChatMessage h2 = new AiChatMessage();
        h2.setRole("assistant");
        h2.setContent("hello");

        LlmChatCommand command = LlmGatewaySupport.toCommand(provider, model, List.of(h1, h2), "next?");

        assertThat(command.baseUrl()).isEqualTo("https://llm.example.com");
        assertThat(command.apiKey()).isEqualTo("sk-secret");
        assertThat(command.modelCode()).isEqualTo("gpt-test");
        assertThat(command.userMessage()).isEqualTo("next?");
        assertThat(command.history())
                .containsExactly(
                        new dev.strata.plugin.api.llm.LlmChatMessage(
                                "system", command.history().get(0).content()),
                        new dev.strata.plugin.api.llm.LlmChatMessage("user", "hi"),
                        new dev.strata.plugin.api.llm.LlmChatMessage("assistant", "hello"));
    }

    @Test
    @DisplayName("空历史时命令历史只含 system 消息")
    void toCommand_empty_history() {
        AiProvider provider = new AiProvider();
        provider.setBaseUrl("u");
        provider.setApiKey("k");
        AiModel model = new AiModel();
        model.setCode("m");

        LlmChatCommand command = LlmGatewaySupport.toCommand(provider, model, List.of(), "only");

        assertThat(command.history()).hasSize(1);
        assertThat(command.history().get(0).role()).isEqualTo("system");
        assertThat(command.userMessage()).isEqualTo("only");
    }

    @Test
    @DisplayName("无附件时 userParts 为 null，保持纯文本向后兼容")
    void toUserParts_no_attachments_is_null() {
        assertThat(LlmGatewaySupport.toUserParts("hi", null)).isNull();
        assertThat(LlmGatewaySupport.toUserParts("hi", List.of())).isNull();
    }

    @Test
    @DisplayName("图片附件渲染为 image_url 段并附带文本段")
    void toUserParts_image_attachment_renders_image_url_part() {
        AiChatAttachment image = new AiChatAttachment("ai-chat", "uploads/a.png", "image/png", "a.png", 1024L);

        List<LlmContentPart> parts = LlmGatewaySupport.toUserParts("看这张图", List.of(image));

        assertThat(parts).hasSize(2);
        assertThat(parts.get(0).type()).isEqualTo("text");
        assertThat(parts.get(0).text()).isEqualTo("看这张图");
        assertThat(parts.get(1).type()).isEqualTo("image_url");
        assertThat(parts.get(1).imageUrl()).isEqualTo("/api/v1/system/files/ai-chat/uploads/a.png/download");
    }

    @Test
    @DisplayName("非图片附件不产生内容段，而是在用户文本末尾追加文件说明行")
    void toUserParts_non_image_attachment_falls_back_to_plain_text() {
        AiChatAttachment pdf = new AiChatAttachment("ai-chat", "docs/b.pdf", "application/pdf", "b.pdf", 2048L);

        List<LlmContentPart> parts = LlmGatewaySupport.toUserParts("总结一下", List.of(pdf));

        assertThat(parts).isNull();
        assertThat(LlmGatewaySupport.augmentTextWithFiles("总结一下", List.of(pdf)))
                .isEqualTo("总结一下\n[已上传文件] b.pdf (2048 字节)");
    }

    @Test
    @DisplayName("纯图片无文本时不产生空文本段")
    void toUserParts_image_only_has_no_text_part() {
        AiChatAttachment image = new AiChatAttachment("ai-chat", "c.jpg", "image/jpeg", "c.jpg", 50L);

        List<LlmContentPart> parts = LlmGatewaySupport.toUserParts("  ", List.of(image));

        assertThat(parts).hasSize(1);
        assertThat(parts.get(0).type()).isEqualTo("image_url");
    }

    @Test
    @DisplayName("1 图 1 文件：图片转 image_url 段，文件以文本说明行拼进用户文本")
    void toUserParts_image_and_file_mixed() {
        AiChatAttachment image = new AiChatAttachment("ai-chat", "pics/x.png", "image/png", "x.png", 12L);
        AiChatAttachment pdf = new AiChatAttachment("ai-chat", "docs/b.pdf", "application/pdf", "b.pdf", 2048L);

        String text = LlmGatewaySupport.augmentTextWithFiles("看下这俩", List.of(image, pdf));
        List<LlmContentPart> parts = LlmGatewaySupport.toUserParts(text, List.of(image, pdf));

        assertThat(parts).hasSize(2);
        assertThat(parts.get(0).type()).isEqualTo("text");
        assertThat(parts.get(0).text()).isEqualTo("看下这俩\n[已上传文件] b.pdf (2048 字节)");
        assertThat(parts.get(1).type()).isEqualTo("image_url");
        assertThat(parts.get(1).imageUrl()).isEqualTo("/api/v1/system/files/ai-chat/pics/x.png/download");
    }

    @Test
    @DisplayName("toCommand 带附件重载把 userParts 透传到命令")
    void toCommand_with_attachments_carries_user_parts() {
        AiProvider provider = new AiProvider();
        provider.setBaseUrl("https://llm");
        provider.setApiKey("sk");
        AiModel model = new AiModel();
        model.setCode("gpt-vision");
        AiChatAttachment image = new AiChatAttachment("ai-chat", "d.png", "image/png", "d.png", 1L);

        LlmChatCommand command = LlmGatewaySupport.toCommand(provider, model, List.of(), "desc", List.of(image));

        assertThat(command.userParts()).hasSize(2);
        assertThat(command.userParts().get(1).type()).isEqualTo("image_url");
    }

    @Test
    @DisplayName("配置了公网基址时 image_url 被前缀成绝对地址")
    void toUserParts_public_base_url_prefixes_absolute_image_url() {
        AiChatAttachment image = new AiChatAttachment("ai-chat", "pics/x.png", "image/png", "x.png", 12L);

        List<LlmContentPart> parts = LlmGatewaySupport.toUserParts("看这张", List.of(image), "https://admin.example.com");

        assertThat(parts).hasSize(2);
        assertThat(parts.get(1).type()).isEqualTo("image_url");
        assertThat(parts.get(1).imageUrl())
                .isEqualTo("https://admin.example.com/api/v1/system/files/ai-chat/pics/x.png/download");
    }

    @Test
    @DisplayName("公网基址末尾的斜杠被归一化，避免双斜杠")
    void toUserParts_public_base_url_trailing_slash_normalised() {
        AiChatAttachment image = new AiChatAttachment("ai-chat", "pics/x.png", "image/png", "x.png", 12L);

        List<LlmContentPart> parts = LlmGatewaySupport.toUserParts("看这张", List.of(image), "https://admin.example.com/");

        assertThat(parts.get(1).imageUrl())
                .isEqualTo("https://admin.example.com/api/v1/system/files/ai-chat/pics/x.png/download");
    }

    @Test
    @DisplayName("公网基址为空白时保持相对路径（本地开发向后兼容）")
    void toUserParts_blank_public_base_url_keeps_relative_path() {
        AiChatAttachment image = new AiChatAttachment("ai-chat", "pics/x.png", "image/png", "x.png", 12L);

        assertThat(LlmGatewaySupport.toUserParts("看这张", List.of(image), null)
                        .get(1)
                        .imageUrl())
                .isEqualTo("/api/v1/system/files/ai-chat/pics/x.png/download");
        assertThat(LlmGatewaySupport.toUserParts("看这张", List.of(image), "  ")
                        .get(1)
                        .imageUrl())
                .isEqualTo("/api/v1/system/files/ai-chat/pics/x.png/download");
    }
}
