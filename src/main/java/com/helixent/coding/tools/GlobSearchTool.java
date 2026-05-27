package com.helixent.coding.tools;

import com.helixent.foundation.tools.Tool;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class GlobSearchTool implements Tool {

    private static final int DEFAULT_LIMIT = 100;
    private static final int DEFAULT_MAX_CHARS = 20000;

    @Override
    public String name() { return "glob_search"; }

    @Override
    public String description() { return "Find files matching a glob pattern"; }

    @Override
    public Map<String, Object> toJsonSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "path", Map.of("type", "string", "description", "The absolute path to the directory to search in"),
                "pattern", Map.of("type", "string", "description", "The glob pattern to match files against"),
                "limit", Map.of("type", "integer", "description", "Maximum results (default 100)"),
                "maxChars", Map.of("type", "integer", "description", "Maximum output characters (default 20000)")
            ),
            "required", List.of("path", "pattern")
        );
    }

    @Override
    public CompletableFuture<Object> invoke(Map<String, Object> input, Object signal) {
        return CompletableFuture.supplyAsync(() -> {
            var pathStr = (String) input.get("path");
            var pattern = (String) input.get("pattern");
            var limit = input.get("limit") instanceof Number n
                ? n.intValue() : DEFAULT_LIMIT;
            var maxChars = input.get("maxChars") instanceof Number n
                ? n.intValue() : DEFAULT_MAX_CHARS;

            var dirCheck = ToolUtils.ensureDirectoryPath(pathStr);
            if (!dirCheck.ok()) {
                var errMeta = new java.util.LinkedHashMap<String, Object>();
                errMeta.put("path", pathStr);
                errMeta.put("pattern", pattern);
                return ToolResultHelper.errorToolResult(dirCheck.error(), "INVALID_DIRECTORY", errMeta);
            }

            try {
                var dir = Path.of(pathStr);
                var matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
                var matches = new ArrayList<String>();

                try (Stream<Path> stream = Files.walk(dir)) {
                    stream.filter(p -> matcher.matches(dir.relativize(p)))
                        .forEach(p -> {
                            if (matches.size() < limit) {
                                matches.add(p.toAbsolutePath().toString());
                            }
                        });
                }

                var joined = String.join("\n", matches);
                var truncated = ToolUtils.truncateText(joined, maxChars);

                return ToolResultHelper.okToolResult(
                    "Found " + matches.size() + " files matching " + pattern,
                    Map.of(
                        "path", pathStr,
                        "pattern", pattern,
                        "matchCount", matches.size(),
                        "truncated", truncated.truncated(),
                        "matches", matches,
                        "content", truncated.text()
                    )
                );
            } catch (Exception e) {
                return ToolResultHelper.errorToolResult(
                    "glob_search failed for pattern " + pattern,
                    "GLOB_SEARCH_FAILED",
                    Map.of("path", pathStr, "pattern", pattern, "message", e.getMessage())
                );
            }
        });
    }
}