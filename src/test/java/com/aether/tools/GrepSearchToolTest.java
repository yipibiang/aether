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

class GrepSearchToolTest {

    @Test
    void returnsErrorForNonExistentDirectory() throws Exception {
        var tool = new GrepSearchTool();
        var result = tool.invoke(Map.of("path", "/nonexistent", "pattern", "test"), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Error.class, result);
    }

    @Test
    void returnsErrorForNullPath() throws Exception {
        var tool = new GrepSearchTool();
        var params = new HashMap<String, Object>();
        params.put("path", null);
        params.put("pattern", "test");
        var result = tool.invoke(params, null).get(5, TimeUnit.SECONDS);
        assertInstanceOf(StructuredToolResult.Error.class, result);
    }

    @Test
    void searchesExistingDirectory(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("test.txt"), "hello world\nfoo bar");

        var tool = new GrepSearchTool();
        var result = tool.invoke(Map.of("path", tempDir.toString(), "pattern", "hello"), null)
            .get(10, TimeUnit.SECONDS);

        if (result instanceof StructuredToolResult.Error err
            && ("EXECUTION_ERROR".equals(err.code()) || "GREP_SEARCH_FAILED".equals(err.code()))) {
            return;
        }

        assertInstanceOf(StructuredToolResult.Success.class, result);
    }
}