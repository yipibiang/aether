package com.helixent.agent;

import com.helixent.foundation.messages.Content;
import com.helixent.foundation.messages.Message;
import com.helixent.foundation.models.ModelContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Agent 中间件 — 在 Agent 循环的各个生命周期节点插入自定义逻辑。
 *
 * <h3>设计模式：中间件链 (Middleware Chain)</h3>
 * 每个钩子方法返回 {@link CompletableFuture}，支持异步处理。
 * 返回 {@code null} 表示"无修改"，返回 Map 则可修改上下文（如注入 prompt）。
 *
 * <h3>JDK21 密封接口</h3>
 * {@link BeforeToolUseResult} 使用 sealed interface 限制返回值类型：
 * 要么跳过工具调用 (Skip)，要么修改上下文 (ContextUpdate)。
 * 调用方可用模式匹配 switch 做穷尽处理。
 */
public interface AgentMiddleware {

    interface AgentContextView {
        String prompt();
        java.util.List<Message> messages();
        java.util.List<com.helixent.foundation.tools.Tool> tools();
        java.util.List<SkillFrontmatter> skills();
        String requestedSkillName();
    }

    default CompletableFuture<Map<String, Object>> beforeModel(ModelContext modelContext, AgentContextView agentContext) {
        return CompletableFuture.completedFuture(null);
    }

    default CompletableFuture<Map<String, Object>> afterModel(AgentContextView agentContext, Message.AssistantMessage message) {
        return CompletableFuture.completedFuture(null);
    }

    default CompletableFuture<Map<String, Object>> beforeAgentRun(AgentContextView agentContext) {
        return CompletableFuture.completedFuture(null);
    }

    default CompletableFuture<Map<String, Object>> afterAgentRun(AgentContextView agentContext) {
        return CompletableFuture.completedFuture(null);
    }

    default CompletableFuture<Void> finalizeAgentStream(AgentContextView agentContext, Throwable error) {
        return CompletableFuture.completedFuture(null);
    }

    default CompletableFuture<Map<String, Object>> beforeAgentStep(AgentContextView agentContext, int step) {
        return CompletableFuture.completedFuture(null);
    }

    default CompletableFuture<Map<String, Object>> afterAgentStep(AgentContextView agentContext, int step) {
        return CompletableFuture.completedFuture(null);
    }

    sealed interface BeforeToolUseResult
        permits BeforeToolUseResult.Skip,
                BeforeToolUseResult.ContextUpdate {

        record Skip(Object result) implements BeforeToolUseResult {}

        record ContextUpdate(Map<String, Object> updates) implements BeforeToolUseResult {}
    }

    default CompletableFuture<BeforeToolUseResult> beforeToolUse(
        AgentContextView agentContext,
        Content.ToolUseContent toolUse
    ) {
        return CompletableFuture.completedFuture(null);
    }

    default CompletableFuture<Map<String, Object>> afterToolUse(
        AgentContextView agentContext,
        Content.ToolUseContent toolUse,
        Object toolResult
    ) {
        return CompletableFuture.completedFuture(null);
    }
}