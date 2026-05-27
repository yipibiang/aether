package com.aether.agent.todos;

import com.aether.agent.AgentMiddleware;
import com.aether.foundation.messages.Content;
import com.aether.foundation.models.ModelContext;
import com.aether.foundation.tools.Tool;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TodoSystem {

    private static final String TODO_WRITE_TOOL_NAME = "todo_write";
    private static final int STEPS_SINCE_WRITE = 10;
    private static final int STEPS_BETWEEN_REMINDERS = 10;

    private final List<TodoItem> store = new ArrayList<>();
    private int stepsSinceLastWrite = Integer.MAX_VALUE;
    private int stepsSinceLastReminder = Integer.MAX_VALUE;

    public record TodoSystemResult(Tool tool, AgentMiddleware middleware) {}

    public static TodoSystemResult create() {
        TodoSystem system = new TodoSystem();
        return new TodoSystemResult(system.createTool(), system.createMiddleware());
    }

    private Tool createTool() {
        return new Tool() {
            @Override
            public String name() {
                return TODO_WRITE_TOOL_NAME;
            }

            @Override
            public String description() {
                return "Create and manage a structured task list for the current session. " +
                    "This helps track progress, organize complex tasks, and demonstrate thoroughness.\n\n" +
                    "## When to Use\n\n" +
                    "1. Complex multi-step tasks requiring 3 or more distinct steps\n" +
                    "2. Non-trivial tasks requiring careful planning or multiple operations\n" +
                    "3. User explicitly requests a todo list\n" +
                    "4. User provides multiple tasks (numbered or comma-separated)\n" +
                    "5. After receiving new instructions — capture requirements as todos (use merge=false to add new ones)\n" +
                    "6. After completing tasks — mark complete with merge=true and add follow-ups\n" +
                    "7. When starting new tasks — mark as in_progress (ideally only one at a time)\n\n" +
                    "## When NOT to Use\n\n" +
                    "1. Single, straightforward tasks\n" +
                    "2. Trivial tasks with no organizational benefit\n" +
                    "3. Tasks completable in fewer than 3 trivial steps\n" +
                    "4. Purely conversational or informational requests\n\n" +
                    "## Task States\n\n" +
                    "- pending: Not yet started\n" +
                    "- in_progress: Currently working on (limit to ONE at a time)\n" +
                    "- completed: Finished successfully\n" +
                    "- cancelled: No longer needed\n\n" +
                    "## Merge Behavior\n\n" +
                    "- merge=true: Merges by id — existing ids are updated, new ids are appended.\n" +
                    "- merge=false: Replaces the entire list with the provided todos.";
            }

            /**
             * 工具：todo_write
             *
             * 参数：
             * {
             *   todos: [
             *     {
             *       id:         字符串 【必须】
             *       content:    字符串 【必须】
             *       status:     字符串 【必须】，只能是 pending / in_progress / completed / cancelled
             *     }
             *   ],
             *   merge: 布尔值 【必须】
             * }
             * @return
             */
            @Override
            public Map<String, Object> toJsonSchema() {
                return Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "todos", Map.of(
                            "type", "array",
                            "items", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                    "id", Map.of("type", "string"),
                                    "content", Map.of("type", "string"),
                                    "status", Map.of("type", "string", "enum", List.of("pending", "in_progress", "completed", "cancelled"))
                                ),
                                "required", List.of("id", "content", "status")
                            )
                        ),
                        "merge", Map.of("type", "boolean")
                    ),
                    "required", List.of("todos", "merge")
                );
            }

            @Override
            @SuppressWarnings("unchecked")
            public CompletableFuture<Object> invoke(Map<String, Object> input, Object signal) {
                List<Map<String, Object>> todos = (List<Map<String, Object>>) input.get("todos");
                boolean merge = Boolean.TRUE.equals(input.get("merge"));

                if (merge) {
                    for (Map<String, Object> item : todos) {
                        String id = (String) item.get("id");
                        String content = (String) item.get("content");
                        TodoStatus status = TodoStatus.valueOf((String) item.get("status"));
                        TodoItem todoItem = new TodoItem(id, content, status);

                        int idx = -1;
                        for (int i = 0; i < store.size(); i++) {
                            if (store.get(i).id().equals(id)) {
                                idx = i;
                                break;
                            }
                        }
                        if (idx >= 0) {
                            store.set(idx, todoItem);
                        } else {
                            store.add(todoItem);
                        }
                    }
                } else {
                    store.clear();
                    for (Map<String, Object> item : todos) {
                        String id = (String) item.get("id");
                        String content = (String) item.get("content");
                        TodoStatus status = TodoStatus.valueOf((String) item.get("status"));
                        store.add(new TodoItem(id, content, status));
                    }
                }

                stepsSinceLastWrite = 0;
                return CompletableFuture.completedFuture(formatSummary());
            }
        };
    }

    private AgentMiddleware createMiddleware() {
        return new AgentMiddleware() {
            @Override
            public CompletableFuture<Map<String, Object>> beforeModel(ModelContext modelContext, AgentMiddleware.AgentContextView agentContext) {
                stepsSinceLastWrite++;
                stepsSinceLastReminder++;

                if (!store.isEmpty()
                    && stepsSinceLastWrite >= STEPS_SINCE_WRITE
                    && stepsSinceLastReminder >= STEPS_BETWEEN_REMINDERS) {
                    stepsSinceLastReminder = 0;
                    String newPrompt = modelContext.prompt() + formatReminder();
                    return CompletableFuture.completedFuture(Map.of("prompt", newPrompt));
                }
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Map<String, Object>> afterToolUse(
                AgentMiddleware.AgentContextView agentContext,
                Content.ToolUseContent toolUse,
                Object toolResult
            ) {
                if (TODO_WRITE_TOOL_NAME.equals(toolUse.name())) {
                    stepsSinceLastWrite = 0;
                }
                return CompletableFuture.completedFuture(null);
            }
        };
    }

    private String formatSummary() {
        int pending = 0, inProgress = 0, completed = 0, cancelled = 0;
        for (TodoItem t : store) {
            switch (t.status()) {
                case pending -> pending++;
                case in_progress -> inProgress++;
                case completed -> completed++;
                case cancelled -> cancelled++;
            }
        }
        List<String> parts = new ArrayList<>();
        if (pending > 0) parts.add(pending + " pending");
        if (inProgress > 0) parts.add(inProgress + " in_progress");
        if (completed > 0) parts.add(completed + " completed");
        if (cancelled > 0) parts.add(cancelled + " cancelled");
        return "Todo list updated. " + store.size() + " items: " + String.join(", ", parts) + ".";
    }

    private String formatReminder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < store.size(); i++) {
            TodoItem t = store.get(i);
            sb.append(i + 1).append(". [").append(t.status()).append("] ").append(t.content()).append("\n");
        }
        return "\n<todo_reminder>\n" +
            "The todo_write tool hasn't been used recently. If you're working on tasks that benefit from tracking, " +
            "consider updating your todo list. Only use it if relevant to the current work. " +
            "Here are the current items:\n\n" +
            sb +
            "</todo_reminder>";
    }
}