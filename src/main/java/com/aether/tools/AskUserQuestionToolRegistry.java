package com.aether.tools;

import com.aether.foundation.tools.Tool;
import com.aether.foundation.tools.ToolRegistry;

import java.util.List;

/** Registers {@link AskUserQuestionTool} for agents that supply an {@link AskUserQuestionManager}. */
public final class AskUserQuestionToolRegistry implements ToolRegistry {

    private final AskUserQuestionTool tool;

    public AskUserQuestionToolRegistry(AskUserQuestionManager manager) {
        if (manager == null) {
            throw new IllegalArgumentException("manager is required");
        }
        this.tool = new AskUserQuestionTool(manager);
    }

    @Override
    public List<Tool> tools() {
        return List.of(tool);
    }
}
