package com.helixent.coding.tools;

import com.helixent.foundation.tools.Tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MkdirTool implements Tool {

    @Override
    public String name() { return "mkdir"; }

    @Override
    public String description() { return "Create a new directory"; }

    @Override
    public Map<String, Object> toJsonSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "path", Map.of("type", "string", "description", "The absolute path of the directory to create")
            ),
            "required", List.of("path")
        );
    }

    @Override
    public CompletableFuture<Object> invoke(Map<String, Object> input, Object signal) {
        return CompletableFuture.supplyAsync(() -> {
            var pathStr = (String) input.get("path");

            if (pathStr == null || pathStr.isBlank()) {
                var errMeta = new java.util.LinkedHashMap<String, Object>();
                errMeta.put("path", pathStr);
                return ToolResultHelper.errorToolResult(
                    "Path is required", "INVALID_PATH", errMeta
                );
            }

            try {
                var path = Path.of(pathStr);
                if (Files.exists(path)) {
                    return ToolResultHelper.errorToolResult(
                        "Path already exists: " + pathStr, "ALREADY_EXISTS",
                        Map.of("path", pathStr)
                    );
                }

                Files.createDirectories(path);

                return ToolResultHelper.okToolResult(
                    "Created directory: " + pathStr,
                    Map.of("path", pathStr)
                );
            } catch (Exception e) {
                return ToolResultHelper.errorToolResult(
                    "Failed to create directory: " + e.getMessage(),
                    "MKDIR_ERROR",
                    Map.of("path", pathStr, "message", e.getMessage())
                );
            }
        });
    }
}