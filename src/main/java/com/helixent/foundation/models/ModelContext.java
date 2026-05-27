package com.helixent.foundation.models;

import com.helixent.foundation.messages.Message;
import com.helixent.foundation.tools.Tool;

import java.util.List;

/**
 * 模型调用上下文 — 封装一次模型请求所需的所有参数。
 *
 * <h3>JDK16 record</h3>
 * record 是不可变数据类，自动生成构造器、访问器、equals/hashCode/toString。
 * 紧凑构造器用于参数校验和默认值。
 */
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