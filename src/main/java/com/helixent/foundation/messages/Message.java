package com.helixent.foundation.messages;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/**
 * 消息模型 — 对话 transcript 的核心类型。
 *
 * <h3>JDK21 密封接口 (sealed interface)</h3>
 * {@code sealed interface} 限制只有 {@code permits} 列出的类才能实现此接口。
 * 编译器在编译期就知道所有可能的子类型，因此配合模式匹配 (pattern matching)
 * 可以在 {@code switch} 中做穷尽检查 (exhaustiveness check)：
 * <pre>{@code
 * switch (message) {
 *     case SystemMessage s -> ...
 *     case UserMessage u    -> ...
 *     case AssistantMessage a -> ...
 *     case ToolMessage t    -> ...
 *     // 不需要 default，编译器保证覆盖所有情况
 * }
 * }</pre>
 * 对比 JDK8：只能用 {@code abstract class} + 包私有构造器来模拟限制子类，
 * 但无法在 switch 中获得编译期穷尽检查。
 */
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

    /**
     * JDK16 record：不可变数据载体，自动生成构造器、访问器、equals/hashCode/toString。
     */
    record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {}

    /**
     * 系统消息：只能是纯文本指令
     * JDK16 record：不可变数据载体。紧凑构造器 (compact constructor) 在字段赋值前执行，
     * 用于参数校验/默认值处理。
     */
    record SystemMessage(java.util.List<Content.TextContent> content) implements Message {
        public SystemMessage {
            if (content == null) content = java.util.List.of();
        }
    }

    /**
     * 用户消息：可以是文本、图片、文件（多模态输入）
     * @param content
     */
    record UserMessage(java.util.List<Content> content) implements Message {
        public UserMessage {
            if (content == null) content = java.util.List.of();
        }
    }

    /**
     * 助理消息：模型输出，可以是文本、图片、工具调用
     * JDK16 record：支持多个构造器重载，通过 this(...) 委托到紧凑构造器。
     */
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