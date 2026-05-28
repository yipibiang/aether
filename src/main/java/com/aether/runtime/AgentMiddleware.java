package com.aether.runtime;

import com.aether.foundation.skills.SkillDescriptor;
import com.aether.foundation.messages.Content;
import com.aether.foundation.messages.Message;
import com.aether.foundation.models.ModelContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Agent ??? ? ? Agent ???????????????????
 *
 * <h3>????????? (Middleware Chain)</h3>
 * ???????? {@link CompletableFuture}????????
 * ?? {@code null} ???????? Map ?????????? prompt??
 * {@link BeforeToolUseResult} ????? (Skip) ?????? (ContextUpdate)?
 */
public interface AgentMiddleware {

    interface AgentContextView {
        String prompt();
        java.util.List<Message> messages();
        java.util.List<com.aether.foundation.tools.Tool> tools();
        java.util.List<SkillDescriptor> skills();
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