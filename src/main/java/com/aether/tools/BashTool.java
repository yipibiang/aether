package com.aether.tools;

import com.aether.foundation.tools.Tool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class BashTool implements Tool {

    private static final int DEFAULT_TIMEOUT_SECONDS = 120;
    private static final int DEFAULT_MAX_CHARS = 20000;

    @Override
    public String name() { return "bash"; }

    @Override
    public String description() { return "Execute a bash command in a subprocess"; }

    @Override
    public Map<String, Object> toJsonSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "command", Map.of("type", "string", "description", "The bash command to execute"),
                "description", Map.of("type", "string", "description", "Brief description of what this command does"),
                "timeout", Map.of("type", "integer", "description", "Timeout in seconds (default 120)"),
                "maxChars", Map.of("type", "integer", "description", "Maximum output characters (default 20000)")
            ),
            "required", List.of("command")
        );
    }

    @Override
    public CompletableFuture<Object> invoke(Map<String, Object> input, Object signal) {
        return CompletableFuture.supplyAsync(() -> {
            var command = (String) input.get("command");
            var description = (String) input.get("description");
            var timeout = input.get("timeout") instanceof Number n
                ? n.intValue() : DEFAULT_TIMEOUT_SECONDS;
            var maxChars = input.get("maxChars") instanceof Number n
                ? n.intValue() : DEFAULT_MAX_CHARS;

            if (command == null || command.isBlank()) {
                var errMeta = new java.util.LinkedHashMap<String, Object>();
                errMeta.put("command", command);
                return ToolResultHelper.errorToolResult(
                    "Command is required", "INVALID_COMMAND", errMeta
                );
            }

            try {
                var isWindows = System.getProperty("os.name").toLowerCase().contains("win");
                List<String> cmdList;
                if (isWindows) {
                    cmdList = List.of("cmd", "/c", command);
                } else {
                    cmdList = List.of("bash", "-c", command);
                }
                var pb = new ProcessBuilder(cmdList);
                pb.redirectErrorStream(true);
                var process = pb.start();

                var output = new StringBuilder();
                try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                var finished = process.waitFor(timeout, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    return ToolResultHelper.errorToolResult(
                        "Command timed out after " + timeout + " seconds",
                        "TIMEOUT",
                        Map.of("command", command, "timeout", timeout)
                    );
                }

                var exitCode = process.exitValue();
                var outputStr = output.toString();
                var truncated = ToolUtils.truncateText(outputStr, maxChars);

                var metadata = new java.util.LinkedHashMap<String, Object>();
                metadata.put("command", command);
                metadata.put("exitCode", exitCode);
                metadata.put("output", truncated.text());
                metadata.put("truncated", truncated.truncated());
                if (description != null) metadata.put("description", description);

                if (exitCode == 0) {
                    return ToolResultHelper.okToolResult(
                        "Command completed successfully (exit code 0)", metadata
                    );
                } else {
                    return ToolResultHelper.errorToolResult(
                        "Command failed with exit code " + exitCode,
                        "NONZERO_EXIT",
                        metadata
                    );
                }
            } catch (Exception e) {
                return ToolResultHelper.errorToolResult(
                    "Command execution failed: " + e.getMessage(),
                    "EXECUTION_ERROR",
                    Map.of("command", command, "message", e.getMessage())
                );
            }
        });
    }
}