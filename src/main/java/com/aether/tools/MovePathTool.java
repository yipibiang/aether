package com.aether.tools;

import com.aether.foundation.tools.Tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MovePathTool implements Tool {

    @Override
    public String name() { return "move_path"; }

    @Override
    public String description() { return "Move or rename a file or directory"; }

    @Override
    public Map<String, Object> toJsonSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "source", Map.of("type", "string", "description", "The absolute source path"),
                "destination", Map.of("type", "string", "description", "The absolute destination path")
            ),
            "required", List.of("source", "destination")
        );
    }

    @Override
    public CompletableFuture<Object> invoke(Map<String, Object> input, Object signal) {
        return CompletableFuture.supplyAsync(() -> {
            var source = (String) input.get("source");
            var destination = (String) input.get("destination");

            if (source == null || source.isBlank()) {
                var errMeta = new java.util.LinkedHashMap<String, Object>();
                errMeta.put("source", source);
                return ToolResultHelper.errorToolResult(
                    "Source path is required", "INVALID_SOURCE", errMeta
                );
            }
            if (destination == null || destination.isBlank()) {
                var errMeta = new java.util.LinkedHashMap<String, Object>();
                errMeta.put("destination", destination);
                return ToolResultHelper.errorToolResult(
                    "Destination path is required", "INVALID_DESTINATION", errMeta
                );
            }

            try {
                var srcPath = Path.of(source);
                var dstPath = Path.of(destination);

                if (!Files.exists(srcPath)) {
                    return ToolResultHelper.errorToolResult(
                        "Source not found: " + source, "NOT_FOUND",
                        Map.of("source", source)
                    );
                }

                var dstParent = dstPath.getParent();
                if (dstParent != null) {
                    Files.createDirectories(dstParent);
                }

                Files.move(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);

                return ToolResultHelper.okToolResult(
                    "Moved " + source + " to " + destination,
                    Map.of("source", source, "destination", destination)
                );
            } catch (Exception e) {
                return ToolResultHelper.errorToolResult(
                    "Failed to move path: " + e.getMessage(),
                    "MOVE_ERROR",
                    Map.of("source", source, "destination", destination, "message", e.getMessage())
                );
            }
        });
    }
}