package com.aether.agent;

import com.aether.foundation.messages.Message;

/** Agent 事件 — 循环中产生，供 UI 订阅渲染。 */
public sealed interface AgentEvent
    permits AgentEvent.MessageEvent,
            AgentEvent.ProgressThinking,
            AgentEvent.ProgressTool {

    record MessageEvent(Message message) implements AgentEvent {}

    record ProgressThinking() implements AgentEvent {}

    record ProgressTool(String name, java.util.Map<String, Object> input) implements AgentEvent {}
}