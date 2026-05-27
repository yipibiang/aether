package com.aether.coding.tools;

import com.aether.foundation.tools.Tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FileInfoTool implements Tool {

    @Override
    public String name() { return "file_info"; }

    @Override
    public String description() { return "Get information about a file or directory"; }

    @Override
    public Map<String, Object> toJsonSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "path", Map.of("type", "string", "description", "The absolute path to get info for")
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
                if (!Files.exists(path)) {
                    return ToolResultHelper.errorToolResult(
                        "Path not found: " + pathStr, "NOT_FOUND",
                        Map.of("path", pathStr)
                    );
                }

                var metadata = new java.util.LinkedHashMap<String, Object>();
                metadata.put("path", pathStr);
                metadata.put("exists", true);
                metadata.put("isDirectory", Files.isDirectory(path));
                metadata.put("isFile", Files.isRegularFile(path));
                metadata.put("size", Files.size(path));
                metadata.put("lastModified", Files.getLastModifiedTime(path).toString());

                if (Files.isRegularFile(path)) {
                    var fileName = path.getFileName().toString();
                    var dotIndex = fileName.lastIndexOf('.');
                    metadata.put("extension", dotIndex > 0 ? fileName.substring(dotIndex + 1) : "");
                }

                return ToolResultHelper.okToolResult(
                    "File info for " + pathStr, metadata
                );
            } catch (Exception e) {
                return ToolResultHelper.errorToolResult(
                    "Failed to get file info: " + e.getMessage(),
                    "FILE_INFO_ERROR",
                    Map.of("path", pathStr, "message", e.getMessage())
                );
            }
        });
    }
}