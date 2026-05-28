package com.aether.tools;

import com.aether.foundation.tools.StructuredToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ReadFileToolTest {

    @Test
    void readsFile(@TempDir Path tempDir) throws Exception {
        var file = tempDir.resolve("test.txt");
        Files.writeString(file, "line1\nline2\nline3");

        var tool = new ReadFileTool();
        var result = tool.invoke(Map.of("file_path", file.toString(), "limit", 2000), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Success.class, result);
        @SuppressWarnings("unchecked")
        var success = (StructuredToolResult.Success<Map<String, Object>>) result;
        assertTrue(((String) success.data().get("content")).contains("line1"));
    }

    @Test
    void readsWithOffset(@TempDir Path tempDir) throws Exception {
        var file = tempDir.resolve("test.txt");
        Files.writeString(file, "line1\nline2\nline3");

        var tool = new ReadFileTool();
        var result = tool.invoke(Map.of("file_path", file.toString(), "limit", 1, "offset", 2), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Success.class, result);
        @SuppressWarnings("unchecked")
        var success = (StructuredToolResult.Success<Map<String, Object>>) result;
        assertTrue(((String) success.data().get("content")).contains("line2"));
    }

    @Test
    void returnsErrorForNonExistentFile() throws Exception {
        var tool = new ReadFileTool();
        var result = tool.invoke(Map.of("file_path", "/nonexistent/file.txt", "limit", 100), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Error.class, result);
    }

    @Test
    void returnsErrorForNullPath() throws Exception {
        var tool = new ReadFileTool();
        var params = new HashMap<String, Object>();
        params.put("file_path", null);
        params.put("limit", 100);
        var result = tool.invoke(params, null).get(5, TimeUnit.SECONDS);
        assertInstanceOf(StructuredToolResult.Error.class, result);
    }
}