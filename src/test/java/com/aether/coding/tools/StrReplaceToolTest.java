package com.aether.coding.tools;

import com.aether.foundation.tools.StructuredToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class StrReplaceToolTest {

    @Test
    void replacesString(@TempDir Path tempDir) throws Exception {
        var file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello world");

        var tool = new StrReplaceTool();
        var result = tool.invoke(Map.of(
            "file_path", file.toString(),
            "old_str", "hello",
            "new_str", "hi"
        ), null).get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Success.class, result);
        assertEquals("hi world", Files.readString(file));
    }

    @Test
    void returnsErrorWhenOldStrNotFound(@TempDir Path tempDir) throws Exception {
        var file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello world");

        var tool = new StrReplaceTool();
        var result = tool.invoke(Map.of(
            "file_path", file.toString(),
            "old_str", "nonexistent",
            "new_str", "replacement"
        ), null).get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Error.class, result);
        assertEquals("NOT_FOUND", ((StructuredToolResult.Error) result).code());
    }

    @Test
    void returnsErrorForMultipleMatches(@TempDir Path tempDir) throws Exception {
        var file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello hello world");

        var tool = new StrReplaceTool();
        var result = tool.invoke(Map.of(
            "file_path", file.toString(),
            "old_str", "hello",
            "new_str", "hi"
        ), null).get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Error.class, result);
        assertEquals("MULTIPLE_MATCHES", ((StructuredToolResult.Error) result).code());
    }

    @Test
    void returnsErrorForNonExistentFile() throws Exception {
        var tool = new StrReplaceTool();
        var result = tool.invoke(Map.of(
            "file_path", "/nonexistent/file.txt",
            "old_str", "a",
            "new_str", "b"
        ), null).get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Error.class, result);
    }
}