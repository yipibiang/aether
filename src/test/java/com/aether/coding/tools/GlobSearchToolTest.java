package com.aether.coding.tools;

import com.aether.foundation.tools.StructuredToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class GlobSearchToolTest {

    @Test
    void findsMatchingFiles(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("test.txt"), "content");
        Files.writeString(tempDir.resolve("test.java"), "code");
        Files.createDirectory(tempDir.resolve("subdir"));

        var tool = new GlobSearchTool();
        var result = tool.invoke(Map.of("path", tempDir.toString(), "pattern", "*.txt"), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Success.class, result);
        @SuppressWarnings("unchecked")
        var success = (StructuredToolResult.Success<Map<String, Object>>) result;
        assertTrue(((String) success.data().get("content")).contains("test.txt"));
    }

    @Test
    void returnsErrorForNonExistentDirectory() throws Exception {
        var tool = new GlobSearchTool();
        var result = tool.invoke(Map.of("path", "/nonexistent", "pattern", "*.txt"), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Error.class, result);
    }

    @Test
    void returnsErrorForNullPath() throws Exception {
        var tool = new GlobSearchTool();
        var params = new HashMap<String, Object>();
        params.put("path", null);
        params.put("pattern", "*.txt");
        var result = tool.invoke(params, null).get(5, TimeUnit.SECONDS);
        assertInstanceOf(StructuredToolResult.Error.class, result);
    }
}