package com.aether.coding.tools;

import com.aether.foundation.tools.Tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WriteFileTool implements Tool {

    @Override
    public String name() { return "write_file"; }

    @Override
    public String description() { return "Write a file to the local filesystem"; }

    @Override
    public Map<String, Object> toJsonSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "file_path", Map.of("type", "string", "description", "The absolute path to the file to write"),
                "content", Map.of("type", "string", "description", "The content to write to the file")
            ),
            "required", List.of("file_path", "content")
        );
    }

    @Override
    public CompletableFuture<Object> invoke(Map<String, Object> input, Object signal) {
        return CompletableFuture.supplyAsync(() -> {
            var filePath = (String) input.get("file_path");
            var content = (String) input.get("content");

            if (filePath == null || filePath.isBlank()) {
                var errMeta = new java.util.LinkedHashMap<String, Object>();
                errMeta.put("file_path", filePath);
                return ToolResultHelper.errorToolResult(
                    "File path is required", "INVALID_PATH", errMeta
                );
            }
            if (content == null) {
                var errMeta = new java.util.LinkedHashMap<String, Object>();
                errMeta.put("file_path", filePath);
                return ToolResultHelper.errorToolResult(
                    "Content is required", "INVALID_CONTENT", errMeta
                );
            }

            try {
                var path = Path.of(filePath);
                var parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                Files.writeString(path, content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);

                var lineCount = content.lines().count();

                return ToolResultHelper.okToolResult(
                    "Wrote " + lineCount + " lines to " + filePath,
                    Map.of("file_path", filePath, "lineCount", lineCount)
                );
            } catch (Exception e) {
                return ToolResultHelper.errorToolResult(
                    "Failed to write file: " + e.getMessage(),
                    "WRITE_ERROR",
                    Map.of("file_path", filePath, "message", e.getMessage())
                );
            }
        });
    }
}