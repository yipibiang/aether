package com.aether.foundation.messages;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/** 消息模型 — 对话 transcript 的核心类型。 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "role")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Message.SystemMessage.class, name = "system"),
    @JsonSubTypes.Type(value = Message.UserMessage.class, name = "user"),
    @JsonSubTypes.Type(value = Message.AssistantMessage.class, name = "assistant"),
    @JsonSubTypes.Type(value = Message.ToolMessage.class, name = "tool")
})
public sealed interface Message
    permits Message.SystemMessage,
            Message.UserMessage,
            Message.AssistantMessage,
            Message.ToolMessage {

    record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {}

    /** 系统消息：只能是纯文本指令。 */
    record SystemMessage(java.util.List<Content.TextContent> content) implements Message {
        public SystemMessage {
            if (content == null) content = java.util.List.of();
        }
    }

    /** 用户消息：文本、图片等多模态输入。 */
    record UserMessage(java.util.List<Content> content) implements Message {
        public UserMessage {
            if (content == null) content = java.util.List.of();
        }
    }

    /** 助理消息：模型输出（文本、思考、工具调用等）。 */
    record AssistantMessage(
        java.util.List<Content> content,
        TokenUsage usage,
        Boolean streaming
    ) implements Message {
        public AssistantMessage {
            if (content == null) content = java.util.List.of();
        }

        public AssistantMessage(java.util.List<Content> content) {
            this(content, null, null);
        }

        public AssistantMessage(java.util.List<Content> content, TokenUsage usage) {
            this(content, usage, null);
        }
    }

    record ToolMessage(java.util.List<Content.ToolResultContent> content) implements Message {
        public ToolMessage {
            if (content == null) content = java.util.List.of();
        }
    }
}