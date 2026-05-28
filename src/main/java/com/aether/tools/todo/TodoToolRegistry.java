package com.aether.tools.todo;

import com.aether.foundation.tools.Tool;
import com.aether.foundation.tools.ToolRegistry;
import com.aether.runtime.AgentMiddleware;

import java.util.List;

/** Todo tools and paired reminder middleware (single shared {@link TodoSystem} state). */
public final class TodoToolRegistry implements ToolRegistry {

    private final TodoSystem system = new TodoSystem();
    private final Tool tool = new TodoWriteTool(system);
    private final AgentMiddleware middleware = new TodoReminderMiddleware(system);

    @Override
    public List<Tool> tools() {
        return List.of(tool);
    }

    public AgentMiddleware middleware() {
        return middleware;
    }
}
