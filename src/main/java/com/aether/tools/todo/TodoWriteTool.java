package com.aether.tools.todo;

import com.aether.foundation.tools.Tool;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** {@link Tool} for session todo list updates; shares state with {@link TodoReminderMiddleware}. */
final class TodoWriteTool implements Tool {

    static final String NAME = "todo_write";

    private final TodoSystem system;

    TodoWriteTool(TodoSystem system) {
        this.system = system;
    }

    @Override
    public String name() {
        return NAME;
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
        system.applyWrite(todos, merge);
        return CompletableFuture.completedFuture(system.formatSummary());
    }
}
