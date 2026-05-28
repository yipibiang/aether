package com.aether.tools;

import com.aether.foundation.tools.Tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ReadFileTool implements Tool {

    private static final int DEFAULT_LIMIT = 2000;
    private static final int DEFAULT_OFFSET = 1;

    @Override
    public String name() { return "read_file"; }

    @Override
    public String description() { return "Read a file from the local filesystem"; }

    @Override
    public Map<String, Object> toJsonSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "file_path", Map.of("type", "string", "description", "The absolute path to the file to read"),
                "limit", Map.of("type", "integer", "description", "The number of lines to read"),
                "offset", Map.of("type", "integer", "description", "The line number to start reading from")
            ),
            "required", List.of("file_path", "limit")
        );
    }

    @Override
    public CompletableFuture<Object> invoke(Map<String, Object> input, Object signal) {
        return CompletableFuture.supplyAsync(() -> {
            var filePath = (String) input.get("file_path");
            var limit = input.get("limit") instanceof Number n
                ? n.intValue() : DEFAULT_LIMIT;
            var offset = input.get("offset") instanceof Number n
                ? n.intValue() : DEFAULT_OFFSET;

            var check = ToolUtils.ensureFilePath(filePath);
            if (!check.ok()) {
                var errMeta = new java.util.LinkedHashMap<String, Object>();
                errMeta.put("file_path", filePath);
                return ToolResultHelper.errorToolResult(check.error(), "INVALID_FILE", errMeta);
            }

            try {
                var path = Path.of(filePath);
                var allLines = Files.readAllLines(path);
                var totalLines = allLines.size();

                if (offset < 1) offset = 1;
                if (offset > totalLines) {
                    return ToolResultHelper.okToolResult(
                        "Offset " + offset + " exceeds file length " + totalLines,
                        Map.of("file_path", filePath, "content", "", "totalLines", totalLines)
                    );
                }

                var endIndex = Math.min(offset - 1 + limit, totalLines);
                var selectedLines = allLines.subList(offset - 1, endIndex);
                var content = new StringBuilder();
                for (int i = 0; i < selectedLines.size(); i++) {
                    content.append(selectedLines.get(i));
                    if (i < selectedLines.size() - 1) content.append("\n");
                }

                return ToolResultHelper.okToolResult(
                    "Read " + selectedLines.size() + " lines from " + filePath,
                    Map.of(
                        "file_path", filePath,
                        "content", content.toString(),
                        "totalLines", totalLines,
                        "offset", offset,
                        "limit", limit
                    )
                );
            } catch (Exception e) {
                return ToolResultHelper.errorToolResult(
                    "Failed to read file: " + e.getMessage(),
                    "READ_ERROR",
                    Map.of("file_path", filePath, "message", e.getMessage())
                );
            }
        });
    }
}