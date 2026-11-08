// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.biz.service;

import dev.strata.ai.chat.api.entity.AiChatMessage;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.api.entity.AiProvider;
import java.util.List;

/** Bundles everything the LLM call needs out of the read-only load. */
record LoadedChat(Long sessionId, AiProvider provider, AiModel model, List<AiChatMessage> history) {}
