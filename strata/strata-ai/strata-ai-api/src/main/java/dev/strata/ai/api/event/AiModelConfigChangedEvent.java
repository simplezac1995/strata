// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.event;

/**
 * AI 模型配置变更事件
 * 当模型配置创建、更新或删除时发布，可用于缓存刷新、通知等
 */
public class AiModelConfigChangedEvent {

    private final String modelId;
    private final String modelCode;
    private final ChangeType changeType;

    public AiModelConfigChangedEvent(String modelId, String modelCode, ChangeType changeType) {
        this.modelId = modelId;
        this.modelCode = modelCode;
        this.changeType = changeType;
    }

    public String getModelId() {
        return modelId;
    }

    public String getModelCode() {
        return modelCode;
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
