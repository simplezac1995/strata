// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.event;

/**
 * AI 对话会话创建事件
 * 当用户创建新的对话会话时发布，可用于通知、统计、缓存更新等
 */
public class AiChatSessionCreatedEvent {

    private final String sessionId;
    private final String userId;
    private final String title;

    public AiChatSessionCreatedEvent(String sessionId, String userId, String title) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.title = title;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }
}
