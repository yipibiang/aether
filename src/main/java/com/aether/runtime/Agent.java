package com.aether.runtime;

import com.aether.foundation.messages.Message;
import com.aether.foundation.models.Model;
import reactor.core.publisher.Flux;

/**
 * Agent 接口 — Agent 循环的抽象，支持 Mock 和独立测试。
 *
 * <h3>设计意图</h3>
 * UI 层依赖此接口而非具体实现，便于：
 * <ul>
 *   <li>单元测试时注入 Mock Agent</li>
 *   <li>替换不同的 Agent 实现（如 ReActAgent、PlanAndExecuteAgent）</li>
 * </ul>
 *
 * <h3>核心方法</h3>
 * <ul>
 *   <li>{@link #stream(Message.UserMessage)} — 流式执行 Agent 循环，推送事件</li>
 *   <li>{@link #abort()} — 中断当前执行</li>
 *   <li>{@link #clearMessages()} — 清空对话历史</li>
 * </ul>
 */
public interface Agent {

    String name();

    Model model();

    boolean isStreaming();

    void clearMessages();

    void abort();

    Flux<AgentEvent> stream(Message.UserMessage userMessage);
}