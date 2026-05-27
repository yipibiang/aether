package com.helixent.agent;

import com.helixent.foundation.messages.Message;

/**
 * Agent 事件 — Agent 循环中产生的事件，用于 UI 层监听和渲染。
 *
 * <h3>JDK21 密封接口 + JDK16 record</h3>
 * 密封接口限制事件类型，record 提供不可变事件数据。
 * UI 层可用模式匹配 switch 穷尽处理所有事件类型。
 */
public sealed interface AgentEvent
    permits AgentEvent.MessageEvent,
            AgentEvent.ProgressThinking,
            AgentEvent.ProgressTool {

    record MessageEvent(Message message) implements AgentEvent {}

    record ProgressThinking() implements AgentEvent {}

    record ProgressTool(String name, java.util.Map<String, Object> input) implements AgentEvent {}
}