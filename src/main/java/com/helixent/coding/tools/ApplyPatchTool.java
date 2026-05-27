package com.helixent.coding.tools;

import com.helixent.foundation.tools.Tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class ApplyPatchTool implements Tool {

    private static final Pattern HUNK_HEADER = Pattern.compile(
        "^@@ -(\\d+),?(\\d*) \\+(\\d+),?(\\d*) @@"
    );

    @Override
    public String name() { return "apply_patch"; }

    @Override
    public String description() { return "Apply a unified diff patch to a file"; }

    @Override
    public Map<String, Object> toJsonSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "file_path", Map.of("type", "string", "description", "The absolute path to the file to patch"),
                "patch", Map.of("type", "string", "description", "The unified diff patch to apply")
            ),
            "required", List.of("file_path", "patch")
        );
    }

    @Override
    public CompletableFuture<Object> invoke(Map<String, Object> input, Object signal) {
        return CompletableFuture.supplyAsync(() -> {
            var filePath = (String) input.get("file_path");
            var patch = (String) input.get("patch");

            if (filePath == null || filePath.isBlank()) {
                var errMeta = new java.util.LinkedHashMap<String, Object>();
                errMeta.put("file_path", filePath);
                return ToolResultHelper.errorToolResult(
                    "File path is required", "INVALID_PATH", errMeta
                );
            }
            if (patch == null || patch.isBlank()) {
                var errMeta = new java.util.LinkedHashMap<String, Object>();
                errMeta.put("file_path", filePath);
                return ToolResultHelper.errorToolResult(
                    "Patch is required", "INVALID_PATCH", errMeta
                );
            }

            try {
                var path = Path.of(filePath);
                var originalLines = Files.exists(path)
                    ? Files.readAllLines(path)
                    : List.<String>of();

                var patchFiles = parsePatch(patch);
                if (patchFiles.isEmpty()) {
                    return ToolResultHelper.errorToolResult(
                        "No valid hunks found in patch", "INVALID_PATCH",
                        Map.of("file_path", filePath)
                    );
                }

                var result = applyHunks(originalLines, patchFiles.get(0).hunks());
                Files.writeString(path, String.join("\n", result) + "\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);

                return ToolResultHelper.okToolResult(
                    "Successfully applied patch to " + filePath,
                    Map.of("file_path", filePath)
                );
            } catch (Exception e) {
                return ToolResultHelper.errorToolResult(
                    "Failed to apply patch: " + e.getMessage(),
                    "PATCH_ERROR",
                    Map.of("file_path", filePath, "message", e.getMessage())
                );
            }
        });
    }

    /**
     * JDK16 record：patch 文件描述（旧路径、新路径、hunk 列表）。
     */
    private record PatchFile(String oldPath, String newPath, List<PatchHunk> hunks) {}

    /**
     * JDK16 record：单个 diff hunk（起止行号、行内容）。
     */
    private record PatchHunk(int oldStart, int oldCount, int newStart, int newCount, List<String> lines) {}

    private static List<PatchFile> parsePatch(String patch) {
        var lines = patch.replace("\r\n", "\n").split("\n");
        var files = new ArrayList<PatchFile>();
        PatchFile current = null;
        PatchHunk currentHunk = null;
        var hunkLines = new ArrayList<String>();

        for (var line : lines) {
            if (line.startsWith("--- ")) {
                if (currentHunk != null && !hunkLines.isEmpty()) {
                    current.hunks().add(new PatchHunk(
                        currentHunk.oldStart(), currentHunk.oldCount(),
                        currentHunk.newStart(), currentHunk.newCount(),
                        List.copyOf(hunkLines)
                    ));
                    hunkLines.clear();
                    currentHunk = null;
                }
                var oldPath = normalizePatchPath(line.substring(4).trim());
                current = new PatchFile(oldPath, "", new ArrayList<>());
                files.add(current);
                continue;
            }
            if (line.startsWith("+++ ") && current != null) {
                current = new PatchFile(current.oldPath(),
                    normalizePatchPath(line.substring(4).trim()), current.hunks());
                files.set(files.size() - 1, current);
                continue;
            }

            var matcher = HUNK_HEADER.matcher(line);
            if (matcher.matches()) {
                if (currentHunk != null && !hunkLines.isEmpty()) {
                    current.hunks().add(new PatchHunk(
                        currentHunk.oldStart(), currentHunk.oldCount(),
                        currentHunk.newStart(), currentHunk.newCount(),
                        List.copyOf(hunkLines)
                    ));
                    hunkLines.clear();
                }
                currentHunk = new PatchHunk(
                    Integer.parseInt(matcher.group(1)),
                    matcher.group(2) != null && !matcher.group(2).isEmpty()
                        ? Integer.parseInt(matcher.group(2)) : 1,
                    Integer.parseInt(matcher.group(3)),
                    matcher.group(4) != null && !matcher.group(4).isEmpty()
                        ? Integer.parseInt(matcher.group(4)) : 1,
                    new ArrayList<>()
                );
                continue;
            }

            if (currentHunk != null) {
                hunkLines.add(line);
            }
        }

        if (currentHunk != null && !hunkLines.isEmpty() && current != null) {
            current.hunks().add(new PatchHunk(
                currentHunk.oldStart(), currentHunk.oldCount(),
                currentHunk.newStart(), currentHunk.newCount(),
                List.copyOf(hunkLines)
            ));
        }

        return files;
    }

    private static String normalizePatchPath(String path) {
        if (path.startsWith("a/")) return path.substring(2);
        if (path.startsWith("b/")) return path.substring(2);
        return path;
    }

    private static List<String> applyHunks(List<String> original, List<PatchHunk> hunks) {
        var result = new ArrayList<>(original);
        var offset = 0;

        for (var hunk : hunks) {
            var oldStart = hunk.oldStart() - 1 + offset;
            var oldCount = hunk.oldCount();
            var newLines = new ArrayList<String>();

            for (var line : hunk.lines()) {
                if (line.startsWith("+")) {
                    newLines.add(line.substring(1));
                } else if (line.startsWith("-")) {
                    // skip
                } else if (line.startsWith(" ")) {
                    newLines.add(line.substring(1));
                }
            }

            if (oldStart <= result.size()) {
                var endIndex = Math.min(oldStart + oldCount, result.size());
                if (oldStart < endIndex) {
                    result.subList(oldStart, endIndex).clear();
                }
                result.addAll(oldStart, newLines);
                offset += newLines.size() - oldCount;
            }
        }

        return result;
    }
}