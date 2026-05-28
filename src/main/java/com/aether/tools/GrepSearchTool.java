package com.aether.tools;

import com.aether.foundation.tools.Tool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class GrepSearchTool implements Tool {

    private static final int DEFAULT_LIMIT = 100;
    private static final int DEFAULT_MAX_CHARS = 20000;

    @Override
    public String name() { return "grep_search"; }

    @Override
    public String description() { return "Search for a pattern in files using ripgrep"; }

    @Override
    public Map<String, Object> toJsonSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "path", Map.of("type", "string", "description", "The absolute path to the directory to search in"),
                "pattern", Map.of("type", "string", "description", "The regex pattern to search for"),
                "glob", Map.of("type", "string", "description", "Glob pattern to filter files"),
                "caseSensitive", Map.of("type", "boolean", "description", "Case sensitive search (default false)"),
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
            var glob = (String) input.get("glob");
            var caseSensitive = Boolean.TRUE.equals(input.get("caseSensitive"));
            var limit = input.get("limit") instanceof Number n
                ? n.intValue() : DEFAULT_LIMIT;
            var maxChars = input.get("maxChars") instanceof Number n
                ? n.intValue() : DEFAULT_MAX_CHARS;

            var dirCheck = ToolUtils.ensureDirectoryPath(pathStr);
            if (!dirCheck.ok()) {
                var errMeta = new java.util.LinkedHashMap<String, Object>();
                errMeta.put("path", pathStr);
                errMeta.put("pattern", pattern);
                errMeta.put("glob", glob);
                return ToolResultHelper.errorToolResult(dirCheck.error(), "INVALID_DIRECTORY", errMeta);
            }

            try {
                var cmd = new ArrayList<String>();
                cmd.add("rg");
                cmd.add("--line-number");
                cmd.add("--no-heading");
                if (!caseSensitive) cmd.add("--ignore-case");
                if (glob != null) {
                    cmd.add("--glob");
                    cmd.add(glob);
                }
                cmd.add(pattern);
                cmd.add(pathStr);

                var pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                var process = pb.start();

                var output = new StringBuilder();
                try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    var lineCount = 0;
                    while ((line = reader.readLine()) != null && lineCount < limit) {
                        output.append(line).append("\n");
                        lineCount++;
                    }
                }

                process.waitFor(30, TimeUnit.SECONDS);
                var outputStr = output.toString();
                var truncated = ToolUtils.truncateText(outputStr, maxChars);

                return ToolResultHelper.okToolResult(
                    "grep_search completed for pattern: " + pattern,
                    Map.of(
                        "path", pathStr,
                        "pattern", pattern,
                        "glob", glob != null ? glob : "",
                        "content", truncated.text(),
                        "truncated", truncated.truncated()
                    )
                );
            } catch (Exception e) {
                var errMeta = new java.util.LinkedHashMap<String, Object>();
                errMeta.put("path", pathStr);
                errMeta.put("pattern", pattern);
                errMeta.put("message", e.getMessage());
                return ToolResultHelper.errorToolResult(
                    "grep_search failed: " + e.getMessage(),
                    "GREP_SEARCH_FAILED",
                    errMeta
                );
            }
        });
    }
}