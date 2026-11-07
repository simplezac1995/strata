// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.event;

/**
 * AI 提供商配置变更事件
 * 当提供商配置创建、更新或删除时发布，可用于缓存刷新、连接测试等
 */
public class AiProviderConfigChangedEvent {

    private final String providerId;
    private final String providerCode;
    private final ChangeType changeType;

    public AiProviderConfigChangedEvent(String providerId, String providerCode, ChangeType changeType) {
        this.providerId = providerId;
        this.providerCode = providerCode;
        this.changeType = changeType;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public enum ChangeType {
        CREATED,
        UPDATED,
        DELETED
    }
}
