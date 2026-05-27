package com.aether.coding.tools;

import com.aether.foundation.tools.Tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class ListFilesTool implements Tool {

    private static final int DEFAULT_MAX_DEPTH = 3;
    private static final int DEFAULT_LIMIT = 200;
    private static final int DEFAULT_MAX_CHARS = 20000;

    @Override
    public String name() { return "list_files"; }

    @Override
    public String description() { return "List files and directories in a given path"; }

    @Override
    public Map<String, Object> toJsonSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "path", Map.of("type", "string", "description", "The absolute path to the directory to list"),
                "ignore", Map.of("type", "array", "items", Map.of("type", "string"),
                    "description", "List of glob patterns to ignore"),
                "maxDepth", Map.of("type", "integer", "description", "Maximum recursion depth (default 3)"),
                "limit", Map.of("type", "integer", "description", "Maximum entries to return (default 200)"),
                "maxChars", Map.of("type", "integer", "description", "Maximum output characters (default 20000)")
            ),
            "required", List.of("path")
        );
    }

    @Override
    public CompletableFuture<Object> invoke(Map<String, Object> input, Object signal) {
        return CompletableFuture.supplyAsync(() -> {
            var pathStr = (String) input.get("path");
            var maxDepth = input.get("maxDepth") instanceof Number n
                ? n.intValue() : DEFAULT_MAX_DEPTH;
            var limit = input.get("limit") instanceof Number n
                ? n.intValue() : DEFAULT_LIMIT;
            var maxChars = input.get("maxChars") instanceof Number n
                ? n.intValue() : DEFAULT_MAX_CHARS;

            var dirCheck = ToolUtils.ensureDirectoryPath(pathStr);
            if (!dirCheck.ok()) {
                var errMeta = new java.util.LinkedHashMap<String, Object>();
                errMeta.put("path", pathStr);
                return ToolResultHelper.errorToolResult(dirCheck.error(), "INVALID_DIRECTORY", errMeta);
            }

            try {
                var dir = Path.of(pathStr);
                var entries = walk(dir, maxDepth);
                var limited = entries.size() > limit
                    ? entries.subList(0, limit)
                    : entries;

                var joined = String.join("\n", limited);
                var truncated = ToolUtils.truncateText(joined, maxChars);

                return ToolResultHelper.okToolResult(
                    "Listed " + limited.size() + " entries in " + pathStr,
                    Map.of(
                        "path", pathStr,
                        "entries", limited,
                        "content", truncated.text(),
                        "truncated", truncated.truncated(),
                        "totalCount", entries.size()
                    )
                );
            } catch (Exception e) {
                return ToolResultHelper.errorToolResult(
                    "Failed to list files: " + e.getMessage(),
                    "LIST_ERROR",
                    Map.of("path", pathStr, "message", e.getMessage())
                );
            }
        });
    }

    private static List<String> walk(Path dir, int maxDepth) throws Exception {
        var entries = new ArrayList<String>();
        walkRecursive(dir, "", 0, maxDepth, entries);
        entries.sort(Comparator.naturalOrder());
        return entries;
    }

    private static void walkRecursive(Path dir, String prefix, int depth, int maxDepth,
                                       List<String> entries) throws Exception {
        try (Stream<Path> stream = Files.list(dir)) {
            var children = stream.sorted().toList();
            for (var child : children) {
                var relativePath = prefix.isEmpty()
                    ? child.getFileName().toString()
                    : prefix + "/" + child.getFileName().toString();
                if (Files.isDirectory(child)) {
                    entries.add(relativePath + "/");
                    if (depth < maxDepth) {
                        walkRecursive(child, relativePath, depth + 1, maxDepth, entries);
                    }
                } else {
                    entries.add(relativePath);
                }
            }
        }
    }
}