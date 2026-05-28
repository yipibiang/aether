package com.aether.runtime;

/** 工具结果截断策略 — 按工具名控制摘要长度与是否附带 data。 */
public record ToolResultPolicy(
    boolean preferSummaryOnly,
    boolean includeData,
    Integer maxStringLength
) {
    private static final ToolResultPolicy DEFAULT = new ToolResultPolicy(false, true, 4000);

    public static ToolResultPolicy forTool(String toolName) {
        return switch (toolName) {
            case "list_files", "glob_search", "grep_search", "file_info", "mkdir", "move_path" ->
                new ToolResultPolicy(true, false, 1000);
            case "read_file" ->
                new ToolResultPolicy(false, true, 12000);
            case "apply_patch", "write_file", "str_replace" ->
                new ToolResultPolicy(false, true, 4000);
            default -> DEFAULT;
        };
    }
}