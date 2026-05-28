package com.aether.tools.todo;

import com.aether.foundation.messages.Content;
import com.aether.foundation.models.ModelContext;
import com.aether.runtime.AgentMiddleware;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Injects todo reminders into the prompt when the list has not been updated recently. */
final class TodoReminderMiddleware implements AgentMiddleware {

    private final TodoSystem system;

    TodoReminderMiddleware(TodoSystem system) {
        this.system = system;
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(
        ModelContext modelContext,
        AgentContextView agentContext
    ) {
        system.onBeforeModelStep();
        var reminder = system.reminderPromptSuffix();
        if (reminder == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(Map.of("prompt", modelContext.prompt() + reminder));
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterToolUse(
        AgentContextView agentContext,
        Content.ToolUseContent toolUse,
        Object toolResult
    ) {
        if (TodoWriteTool.NAME.equals(toolUse.name())) {
            system.onTodoWriteUsed();
        }
        return CompletableFuture.completedFuture(null);
    }
}
