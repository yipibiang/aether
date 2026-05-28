package com.aether.tools;

import com.aether.foundation.tools.Tool;
import com.aether.foundation.tools.ToolRegistry;

import java.util.List;

/** Filesystem / shell tool bundle for the coding agent scenario. Each entry is a separate {@link Tool} implementation. */
public final class CodingToolRegistry implements ToolRegistry {

    @Override
    public List<Tool> tools() {
        return List.of(
            new BashTool(),
            new FileInfoTool(),
            new ListFilesTool(),
            new GlobSearchTool(),
            new GrepSearchTool(),
            new MkdirTool(),
            new MovePathTool(),
            new ReadFileTool(),
            new WriteFileTool(),
            new StrReplaceTool(),
            new ApplyPatchTool()
        );
    }
}
