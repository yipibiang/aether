package com.aether.coding.tools;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ToolUtils {

    private ToolUtils() {}

    public record PathCheck(boolean ok, String error) {}

    public static PathCheck ensureFilePath(String path) {
        if (path == null || path.isBlank()) {
            return new PathCheck(false, "Path is required");
        }
        var p = Path.of(path);
        if (!Files.exists(p)) {
            return new PathCheck(false, "File not found: " + path);
        }
        if (!Files.isRegularFile(p)) {
            return new PathCheck(false, "Path is not a file: " + path);
        }
        return new PathCheck(true, null);
    }

    public static PathCheck ensureDirectoryPath(String path) {
        if (path == null || path.isBlank()) {
            return new PathCheck(false, "Path is required");
        }
        var p = Path.of(path);
        if (!Files.exists(p)) {
            return new PathCheck(false, "Directory not found: " + path);
        }
        if (!Files.isDirectory(p)) {
            return new PathCheck(false, "Path is not a directory: " + path);
        }
        return new PathCheck(true, null);
    }

    public record TruncatedText(String text, boolean truncated) {}

    public static TruncatedText truncateText(String text, int maxChars) {
        if (text == null) return new TruncatedText("", false);
        if (text.length() <= maxChars) return new TruncatedText(text, false);
        var truncated = text.substring(0, maxChars);
        return new TruncatedText(
            truncated + "\n... (truncated, total " + text.length() + " chars)",
            true
        );
    }
}