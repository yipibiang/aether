package com.helixent.agent;

/**
 * JDK16 record：工具结果截断策略。
 *
 * <h3>JDK14 switch 表达式</h3>
 * {@code switch} 表达式用 {@code ->} 箭头语法，无需 {@code break}，
 * 每个分支直接产生返回值。对比 JDK8 只能用 switch 语句 + break。
 */
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