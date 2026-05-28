package com.aether.tools;

import com.aether.foundation.tools.Tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class StrReplaceTool implements Tool {

    @Override
    public String name() { return "str_replace"; }

    @Override
    public String description() { return "Perform exact string replacements in an existing file"; }

    @Override
    public Map<String, Object> toJsonSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "file_path", Map.of("type", "string", "description", "The absolute path to the file to modify"),
                "old_str", Map.of("type", "string", "description", "The text to replace"),
                "new_str", Map.of("type", "string", "description", "The text to replace it with")
            ),
            "required", List.of("file_path", "old_str", "new_str")
        );
    }

    @Override
    public CompletableFuture<Object> invoke(Map<String, Object> input, Object signal) {
        return CompletableFuture.supplyAsync(() -> {
            var filePath = (String) input.get("file_path");
            var oldStr = (String) input.get("old_str");
            var newStr = (String) input.get("new_str");

            var check = ToolUtils.ensureFilePath(filePath);
            if (!check.ok()) {
                return ToolResultHelper.errorToolResult(check.error(), "INVALID_FILE",
                    Map.of("file_path", filePath));
            }

            try {
                var path = Path.of(filePath);
                var content = Files.readString(path);

                var count = 0;
                var index = 0;
                while ((index = content.indexOf(oldStr, index)) != -1) {
                    count++;
                    index += oldStr.length();
                }

                if (count == 0) {
                    return ToolResultHelper.errorToolResult(
                        "old_str not found in file", "NOT_FOUND",
                        Map.of("file_path", filePath, "old_str", oldStr)
                    );
                }
                if (count > 1) {
                    return ToolResultHelper.errorToolResult(
                        "old_str found " + count + " times in file, must be unique",
                        "MULTIPLE_MATCHES",
                        Map.of("file_path", filePath, "old_str", oldStr, "count", count)
                    );
                }

                var newContent = content.replace(oldStr, newStr);
                Files.writeString(path, newContent,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);

                return ToolResultHelper.okToolResult(
                    "Successfully replaced string in " + filePath,
                    Map.of("file_path", filePath)
                );
            } catch (Exception e) {
                return ToolResultHelper.errorToolResult(
                    "Failed to replace string: " + e.getMessage(),
                    "REPLACE_ERROR",
                    Map.of("file_path", filePath, "message", e.getMessage())
                );
            }
        });
    }
}