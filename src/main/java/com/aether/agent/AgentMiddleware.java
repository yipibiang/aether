package com.aether.agent;

import com.aether.foundation.messages.Content;
import com.aether.foundation.messages.Message;
import com.aether.foundation.models.ModelContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Agent 中间件 — 在 Agent 循环的各个生命周期节点插入自定义逻辑。
 *
 * <h3>设计模式：中间件链 (Middleware Chain)</h3>
 * 每个钩子方法返回 {@link CompletableFuture}，支持异步处理。
 * 返回 {@code null} 表示无修改；返回 Map 可修改上下文（如注入 prompt）。
 * {@link BeforeToolUseResult} 为跳过工具 (Skip) 或更新上下文 (ContextUpdate)。
 */
public interface AgentMiddleware {

    interface AgentContextView {
        String prompt();
        java.util.List<Message> messages();
        java.util.List<com.aether.foundation.tools.Tool> tools();
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