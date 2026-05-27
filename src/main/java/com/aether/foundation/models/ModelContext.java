package com.aether.foundation.models;

import com.aether.foundation.messages.Message;
import com.aether.foundation.tools.Tool;

import java.util.List;

/** 模型调用上下文 — 一次请求所需的 prompt、消息、工具等。 */
public record ModelContext(
    String prompt,
    List<Message> messages,
    List<Tool> tools,
    Object signal
) {
    public ModelContext {
        if (prompt == null || prompt.trim().isEmpty()) throw new IllegalArgumentException("prompt must not be null");
        if (messages == null) messages = List.of();
    }
}